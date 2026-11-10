package com.hospital.management.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AutomationService {
    private static final Logger log = LoggerFactory.getLogger(AutomationService.class);
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");

    private final JdbcTemplate db;
    private final AuditService audit;

    private final AtomicLong totalCycles = new AtomicLong(0);
    private final AtomicLong totalOverdueReconciled = new AtomicLong(0);
    private final AtomicLong totalInvoicesGenerated = new AtomicLong(0);
    private final AtomicLong totalRemindersDispatched = new AtomicLong(0);
    private volatile OffsetDateTime lastRunAt = null;

    private final Deque<Map<String, Object>> recentActivityLogs = new ConcurrentLinkedDeque<>();
    private static final int MAX_LOGS = 25;

    public AutomationService(JdbcTemplate db, AuditService audit) {
        this.db = db;
        this.audit = audit;
    }

    /**
     * Executes every 60 seconds automatically.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    public void scheduledAutomationRun() {
        try {
            runAutomationCycle();
        } catch (Exception e) {
            log.error("Clinical automation cycle encountered an error: {}", e.getMessage(), e);
            recordLog("ERROR", "Automation cycle error: " + e.getMessage());
        }
    }

    /**
     * Executes one complete clinical automation cycle.
     */
    @Transactional
    public synchronized Map<String, Object> runAutomationCycle() {
        OffsetDateTime now = OffsetDateTime.now(CLINIC_ZONE);
        lastRunAt = now;
        totalCycles.incrementAndGet();

        Long systemActorId = getSystemActorId();

        // 1. Reconcile overdue unfulfilled appointments to NO_SHOW
        int overdueCount = reconcileOverdueAppointments(now, systemActorId);

        // 2. Auto-generate billing invoices for finalized encounters without bills
        int billedCount = autoGenerateInvoicesForFinalizedEncounters(systemActorId);

        // 3. Scan inventory stock and impending expiries
        List<Map<String, Object>> expiringBatches = scanExpiringBatches();
        List<Map<String, Object>> lowStockItems = scanLowStockItems();

        // 4. Simulate patient visit reminders for appointments in next 24 hours
        int remindersCount = dispatchUpcomingVisitReminders(now);

        String summary = String.format("Reconciled %d overdue appointments, auto-billed %d encounters, scanned %d expiring batches, %d low-stock items, dispatched %d reminders.",
                overdueCount, billedCount, expiringBatches.size(), lowStockItems.size(), remindersCount);
        recordLog("CYCLE_COMPLETED", summary);

        Map<String, Object> result = new HashMap<>();
        result.put("cycleTimestamp", now.toString());
        result.put("overdueReconciled", overdueCount);
        result.put("invoicesGenerated", billedCount);
        result.put("expiringBatchesCount", expiringBatches.size());
        result.put("lowStockItemsCount", lowStockItems.size());
        result.put("remindersDispatched", remindersCount);
        result.put("summary", summary);
        return result;
    }

    private int reconcileOverdueAppointments(OffsetDateTime now, Long actorId) {
        String query = """
            SELECT a.id, a.status, a.patient_id, a.doctor_id, a.starts_at, a.ends_at
            FROM appointments a
            WHERE a.status IN ('BOOKED', 'CHECKED_IN')
              AND a.ends_at < ?
        """;
        List<Map<String, Object>> overdue = db.queryForList(query, now);
        int count = 0;

        for (Map<String, Object> appt : overdue) {
            Long apptId = ((Number) appt.get("id")).longValue();
            String prevStatus = (String) appt.get("status");

            db.update("UPDATE appointments SET status = 'NO_SHOW', updated_at = CURRENT_TIMESTAMP WHERE id = ?", apptId);
            db.update("INSERT INTO appointment_status_history (appointment_id, previous_status, new_status, reason, changed_by) VALUES (?, ?, 'NO_SHOW', 'Clinical Automation: Visit window elapsed without consultation', ?)",
                    apptId, prevStatus, actorId);

            audit.record("AUTO_RECONCILE_NO_SHOW", "APPOINTMENT", apptId, "Status updated from " + prevStatus + " to NO_SHOW automatically");
            count++;
            totalOverdueReconciled.incrementAndGet();
        }

        if (count > 0) {
            recordLog("OVERDUE_RECONCILED", "Auto-reconciled " + count + " lapsed appointment(s) to NO_SHOW.");
        }
        return count;
    }

    private int autoGenerateInvoicesForFinalizedEncounters(Long actorId) {
        String query = """
            SELECT e.id AS encounter_id, e.patient_id
            FROM encounters e
            WHERE e.status = 'FINALIZED'
              AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.encounter_id = e.id)
            LIMIT 50
        """;
        List<Map<String, Object>> unbilled = db.queryForList(query);
        int count = 0;

        for (Map<String, Object> enc : unbilled) {
            Long encId = ((Number) enc.get("encounter_id")).longValue();
            Long patientId = ((Number) enc.get("patient_id")).longValue();
            String invNum = "INV-AUTO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

            KeyHolder keys = new GeneratedKeyHolder();
            db.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO invoices (invoice_number, encounter_id, patient_id, status, created_by) VALUES (?, ?, ?, 'OPEN', ?)",
                        new String[] { "id" });
                ps.setString(1, invNum);
                ps.setLong(2, encId);
                ps.setLong(3, patientId);
                ps.setLong(4, actorId);
                return ps;
            }, keys);

            Long invId = extractGeneratedId(keys);
            if (invId != null) {
                db.update("INSERT INTO invoice_items (invoice_id, item_type, description_snapshot, quantity, unit_price_snapshot) VALUES (?, 'SERVICE', 'Doctor Outpatient Consultation (Auto-generated)', 1, 500.00)",
                        invId);
                audit.record("AUTO_INVOICE_GENERATED", "INVOICE", invId, "Generated invoice " + invNum + " for encounter #" + encId);
                count++;
                totalInvoicesGenerated.incrementAndGet();
            }
        }

        if (count > 0) {
            recordLog("AUTO_BILLING", "Auto-generated " + count + " consultation invoice(s) for finalized encounters.");
        }
        return count;
    }

    private List<Map<String, Object>> scanExpiringBatches() {
        LocalDate threshold = LocalDate.now().plusDays(60);
        String sql = """
            SELECT b.id, b.batch_number, b.expiry_date, b.quantity_on_hand,
                   m.generic_name, m.brand_name
            FROM medicine_batches b
            JOIN medicines m ON m.id = b.medicine_id
            WHERE b.quantity_on_hand > 0 AND b.expiry_date <= ?
            ORDER BY b.expiry_date ASC
        """;
        return db.queryForList(sql, threshold);
    }

    private List<Map<String, Object>> scanLowStockItems() {
        String sql = """
            SELECT m.id, m.code, m.generic_name, m.reorder_level,
                   COALESCE(SUM(b.quantity_on_hand), 0) AS total_stock
            FROM medicines m
            LEFT JOIN medicine_batches b ON b.medicine_id = m.id
            WHERE m.active = TRUE
            GROUP BY m.id, m.code, m.generic_name, m.reorder_level
            HAVING COALESCE(SUM(b.quantity_on_hand), 0) <= m.reorder_level
            ORDER BY total_stock ASC
        """;
        return db.queryForList(sql);
    }

    private int dispatchUpcomingVisitReminders(OffsetDateTime now) {
        OffsetDateTime in24Hours = now.plusHours(24);
        String sql = """
            SELECT a.id, a.starts_at, p.full_name, p.phone, s.display_name AS doctor_name
            FROM appointments a
            JOIN patients p ON p.id = a.patient_id
            JOIN doctors d ON d.id = a.doctor_id
            JOIN staff_users s ON s.id = d.staff_user_id
            WHERE a.status = 'BOOKED'
              AND a.starts_at BETWEEN ? AND ?
        """;
        List<Map<String, Object>> upcoming = db.queryForList(sql, now, in24Hours);
        int count = upcoming.size();
        totalRemindersDispatched.addAndGet(count);
        return count;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("engineStatus", "ACTIVE");
        status.put("cadence", "60 seconds");
        status.put("lastRunAt", lastRunAt != null ? lastRunAt.toString() : "Pending Initial Trigger");
        status.put("totalCyclesExecuted", totalCycles.get());
        status.put("totalOverdueReconciled", totalOverdueReconciled.get());
        status.put("totalInvoicesGenerated", totalInvoicesGenerated.get());
        status.put("totalRemindersDispatched", totalRemindersDispatched.get());

        List<Map<String, Object>> expiring = scanExpiringBatches();
        List<Map<String, Object>> lowStock = scanLowStockItems();

        status.put("activeExpiringBatchesCount", expiring.size());
        status.put("activeLowStockItemsCount", lowStock.size());
        status.put("expiringBatchesAlerts", expiring);
        status.put("lowStockAlerts", lowStock);
        status.put("recentLogs", new ArrayList<>(recentActivityLogs));
        return status;
    }

    private void recordLog(String eventType, String message) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", OffsetDateTime.now(CLINIC_ZONE).toString());
        logEntry.put("eventType", eventType);
        logEntry.put("message", message);

        recentActivityLogs.addFirst(logEntry);
        while (recentActivityLogs.size() > MAX_LOGS) {
            recentActivityLogs.removeLast();
        }
    }

    private Long getSystemActorId() {
        List<Long> ids = db.queryForList("SELECT id FROM staff_users WHERE role = 'ADMIN' AND active = TRUE ORDER BY id ASC LIMIT 1", Long.class);
        return ids.isEmpty() ? 1L : ids.get(0);
    }

    private Long extractGeneratedId(KeyHolder keys) {
        if (keys.getKeys() != null) {
            for (Map.Entry<String, Object> entry : keys.getKeys().entrySet()) {
                if (entry.getKey().equalsIgnoreCase("id") && entry.getValue() instanceof Number n) {
                    return n.longValue();
                }
            }
        }
        Number key = keys.getKey();
        return key != null ? key.longValue() : null;
    }
}
