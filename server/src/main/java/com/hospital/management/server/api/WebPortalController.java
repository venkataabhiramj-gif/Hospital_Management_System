package com.hospital.management.server.api;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api")
public class WebPortalController {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");
    private final JdbcTemplate db;
    private final WorkflowController workflow;

    public WebPortalController(JdbcTemplate db, WorkflowController workflow) {
        this.db = db;
        this.workflow = workflow;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Integer patients = db.queryForObject("SELECT COUNT(*) FROM patients WHERE active=TRUE", Integer.class);
        Integer doctors = db.queryForObject("SELECT COUNT(*) FROM doctors WHERE active=TRUE", Integer.class);
        Integer appointments = db.queryForObject("SELECT COUNT(*) FROM appointments WHERE status NOT IN ('CANCELLED', 'NO_SHOW')", Integer.class);
        BigDecimal revenue = db.queryForObject("SELECT COALESCE(SUM(amount - refund_amount), 0) FROM payments", BigDecimal.class);

        return Map.of(
                "patients", patients != null ? patients : 0,
                "doctors", doctors != null ? doctors : 0,
                "appointments", appointments != null ? appointments : 0,
                "revenue", revenue != null ? revenue : BigDecimal.ZERO
        );
    }

    @PostMapping("/doctors/web")
    @Transactional
    public Map<String, Object> addDoctorWeb(@RequestBody Map<String, Object> body) {
        String name = String.valueOf(body.get("name"));
        String spec = String.valueOf(body.getOrDefault("specialization", "General Medicine"));
        String username = "doc." + name.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (username.length() > 30) username = username.substring(0, 30);
        if (username.length() < 5) username = username + UUID.randomUUID().toString().substring(0, 4);

        Integer count = db.queryForObject("SELECT COUNT(*) FROM staff_users WHERE username = ?", Integer.class, username);
        if (count != null && count > 0) {
            username = username + (System.currentTimeMillis() % 1000);
        }

        // Demo password hash for standard staff
        Long staffId = insert("INSERT INTO staff_users (username, password_hash, display_name, role) VALUES (?, ?, ?, 'DOCTOR')",
                username, "$2a$12$e8wFw0YyQd78Z0Qc0aVvauL8O8bF7uC8WnS3yTqjJm9F.P7e4wVqW", name);

        Long docId = insert("INSERT INTO doctors (staff_user_id, specialty, appointment_minutes) VALUES (?, ?, 20)",
                staffId, spec);

        for (int day = 1; day <= 7; day++) {
            db.update("INSERT INTO doctor_schedule (doctor_id, day_of_week, starts_at, ends_at) VALUES (?, ?, '09:00:00', '17:00:00')",
                    docId, day);
        }

        return Map.of("message", "Medical specialist registered successfully", "id", docId);
    }

    @DeleteMapping("/patients")
    @Transactional
    public Map<String, Object> deletePatient(@RequestParam String id) {
        int updated = db.update("UPDATE patients SET active=FALSE, updated_at=CURRENT_TIMESTAMP WHERE patient_number=? OR CAST(id AS VARCHAR)=?", id, id);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found");
        }
        return Map.of("message", "Patient record archived successfully", "id", id);
    }

    @GetMapping("/bills")
    public List<Map<String, Object>> bills() {
        String sql = """
            SELECT i.id AS bill_id,
                   i.invoice_number AS invoice_number,
                   p.patient_number AS patient_id,
                   p.full_name AS patient_name,
                   COALESCE((SELECT SUM(quantity * unit_price_snapshot) FROM invoice_items x WHERE x.invoice_id = i.id AND item_type = 'SERVICE'), 500.00) AS consultation_fee,
                   0.00 AS treatment_charges,
                   COALESCE((SELECT SUM(quantity * unit_price_snapshot) FROM invoice_items x WHERE x.invoice_id = i.id AND item_type = 'MEDICINE'), 0.00) AS medicine_charges,
                   0 AS discount_percent,
                   5.0 AS tax_percent,
                   COALESCE((SELECT SUM(quantity * unit_price_snapshot) FROM invoice_items x WHERE x.invoice_id = i.id), 500.00) AS total_amount,
                   CAST(i.created_at AS DATE) AS billing_date,
                   CASE WHEN i.status = 'PAID' THEN 'PAID' ELSE 'PENDING' END AS payment_status
            FROM invoices i
            JOIN patients p ON p.id = i.patient_id
            ORDER BY i.created_at DESC
        """;
        List<Map<String, Object>> rows = db.queryForList(sql);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("billId", ((Number) r.get("bill_id")).longValue());
            m.put("invoiceNumber", r.get("invoice_number"));
            m.put("patientId", r.get("patient_id"));
            m.put("patientName", r.get("patient_name"));
            m.put("consultationFee", r.get("consultation_fee"));
            m.put("treatmentCharges", r.get("treatment_charges"));
            m.put("medicineCharges", r.get("medicine_charges"));
            m.put("discountPercent", r.get("discount_percent"));
            m.put("taxPercent", r.get("tax_percent"));
            m.put("totalAmount", r.get("total_amount"));
            m.put("billingDate", r.get("billing_date") != null ? r.get("billing_date").toString() : LocalDate.now().toString());
            m.put("paymentStatus", r.get("payment_status"));
            result.add(m);
        }
        return result;
    }

    @PostMapping("/bills")
    @Transactional
    public Map<String, Object> handleBillAction(@RequestBody Map<String, Object> body) {
        String action = (String) body.get("action");
        Long staff = workflow.currentStaffId();

        if ("markPaid".equalsIgnoreCase(action)) {
            Object billIdObj = body.get("billId");
            if (billIdObj == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing billId");
            }
            long invoiceId = Long.parseLong(billIdObj.toString());

            BigDecimal total = db.queryForObject(
                    "SELECT COALESCE(SUM(quantity * unit_price_snapshot), 500.00) FROM invoice_items WHERE invoice_id = ?",
                    BigDecimal.class, invoiceId);
            BigDecimal paid = db.queryForObject(
                    "SELECT COALESCE(SUM(amount - refund_amount), 0) FROM payments WHERE invoice_id = ?",
                    BigDecimal.class, invoiceId);

            BigDecimal due = total.subtract(paid);
            if (due.compareTo(BigDecimal.ZERO) > 0) {
                String receipt = "RCP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
                db.update(
                        "INSERT INTO payments (receipt_number, invoice_id, amount, method, received_by) VALUES (?, ?, ?, 'UPI', ?)",
                        receipt, invoiceId, due, staff);
            }
            db.update("UPDATE invoices SET status = 'PAID' WHERE id = ?", invoiceId);
            return Map.of("message", "Payment recorded successfully", "billId", invoiceId);
        }

        // Create new bill
        String patientIdStr = String.valueOf(body.get("patientId"));
        Long patientDbId = db.query(
                "SELECT id FROM patients WHERE (patient_number = ? OR CAST(id AS VARCHAR) = ?) AND active = TRUE",
                rs -> rs.next() ? rs.getLong(1) : null, patientIdStr, patientIdStr);
        if (patientDbId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }

        Long doctorDbId = db.query("SELECT id FROM doctors WHERE active = TRUE LIMIT 1", rs -> rs.next() ? rs.getLong(1) : null);
        if (doctorDbId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No active doctor found in system");
        }

        // Create appointment and encounter for the billing episode
        OffsetDateTime now = OffsetDateTime.now(CLINIC_ZONE);
        Long apptId = insert("INSERT INTO appointments(patient_id, doctor_id, starts_at, ends_at, visit_type, status, reason, booked_by) VALUES(?,?,?,?, 'WALK_IN', 'COMPLETED', 'Point-of-care outpatient consultation', ?)",
                patientDbId, doctorDbId, now, now.plusMinutes(20), staff);

        Long encId = insert("INSERT INTO encounters(appointment_id, patient_id, doctor_id, complaint, clinical_notes, diagnosis_text, status, created_by, finalized_at) VALUES(?,?,?, 'Consultation completed', 'Routine checkup', 'General Outpatient Care', 'FINALIZED', ?, CURRENT_TIMESTAMP)",
                apptId, patientDbId, doctorDbId, staff);

        String invoiceNumber = "INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String initialStatus = "PAID".equalsIgnoreCase(String.valueOf(body.get("paymentStatus"))) ? "PAID" : "OPEN";

        Long invoiceId = insert("INSERT INTO invoices(invoice_number, encounter_id, patient_id, status, created_by) VALUES(?,?,?,?,?)",
                invoiceNumber, encId, patientDbId, initialStatus, staff);

        BigDecimal consultFee = new BigDecimal(String.valueOf(body.getOrDefault("consultationFee", "500")));
        BigDecimal treatmentFee = new BigDecimal(String.valueOf(body.getOrDefault("treatmentCharges", "0")));
        BigDecimal medicineFee = new BigDecimal(String.valueOf(body.getOrDefault("medicineCharges", "0")));

        if (consultFee.compareTo(BigDecimal.ZERO) > 0) {
            db.update("INSERT INTO invoice_items(invoice_id, item_type, description_snapshot, quantity, unit_price_snapshot) VALUES(?,'SERVICE','Doctor Consultation',1,?)",
                    invoiceId, consultFee);
        }
        if (treatmentFee.compareTo(BigDecimal.ZERO) > 0) {
            db.update("INSERT INTO invoice_items(invoice_id, item_type, description_snapshot, quantity, unit_price_snapshot) VALUES(?,'SERVICE','Treatment & Procedure Fee',1,?)",
                    invoiceId, treatmentFee);
        }
        if (medicineFee.compareTo(BigDecimal.ZERO) > 0) {
            db.update("INSERT INTO invoice_items(invoice_id, item_type, description_snapshot, quantity, unit_price_snapshot) VALUES(?,'MEDICINE','Prescribed Dispensary Items',1,?)",
                    invoiceId, medicineFee);
        }

        BigDecimal subtotal = consultFee.add(treatmentFee).add(medicineFee);
        if ("PAID".equals(initialStatus) && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            String receipt = "RCP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            db.update("INSERT INTO payments(receipt_number, invoice_id, amount, method, received_by) VALUES(?,?,?,'CASH',?)",
                    receipt, invoiceId, subtotal, staff);
        }

        return Map.of("message", "Invoice generated successfully", "billId", invoiceId, "invoiceNumber", invoiceNumber);
    }

    @GetMapping("/records")
    public List<Map<String, Object>> records() {
        String sql = """
            SELECT e.id AS record_id,
                   p.patient_number AS patient_id,
                   p.full_name AS patient_name,
                   d.id AS doctor_id,
                   s.display_name AS doctor_name,
                   COALESCE(e.diagnosis_text, 'General Outpatient Indication') AS diagnosis,
                   COALESCE(e.current_medicines, 'Tab. Paracetamol 500mg PO PRN') AS prescription,
                   COALESCE(e.complaint, 'Vitals and symptom evaluation') AS lab_tests,
                   COALESCE(e.clinical_notes, 'Routine follow-up prescribed.') AS notes,
                   CAST(e.created_at AS DATE) AS record_date
            FROM encounters e
            JOIN patients p ON p.id = e.patient_id
            JOIN doctors d ON d.id = e.doctor_id
            JOIN staff_users s ON s.id = d.staff_user_id
            ORDER BY e.created_at DESC
        """;
        List<Map<String, Object>> rows = db.queryForList(sql);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("recordId", ((Number) r.get("record_id")).longValue());
            m.put("patientId", r.get("patient_id"));
            m.put("patientName", r.get("patient_name"));
            m.put("doctorId", ((Number) r.get("doctor_id")).longValue());
            m.put("doctorName", r.get("doctor_name"));
            m.put("diagnosis", r.get("diagnosis"));
            m.put("prescription", r.get("prescription"));
            m.put("labTests", r.get("lab_tests"));
            m.put("notes", r.get("notes"));
            m.put("recordDate", r.get("record_date") != null ? r.get("record_date").toString() : LocalDate.now().toString());
            result.add(m);
        }
        return result;
    }

    @PostMapping("/records")
    @Transactional
    public Map<String, Object> addRecord(@RequestBody Map<String, Object> body) {
        Long staff = workflow.currentStaffId();
        String patientIdStr = String.valueOf(body.get("patientId"));
        Long patientDbId = db.query(
                "SELECT id FROM patients WHERE (patient_number = ? OR CAST(id AS VARCHAR) = ?) AND active = TRUE",
                rs -> rs.next() ? rs.getLong(1) : null, patientIdStr, patientIdStr);
        if (patientDbId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }

        Long doctorDbId;
        Object docIdObj = body.get("doctorId");
        if (docIdObj != null && !String.valueOf(docIdObj).isBlank()) {
            doctorDbId = Long.parseLong(String.valueOf(docIdObj));
        } else {
            doctorDbId = db.query("SELECT id FROM doctors WHERE active = TRUE LIMIT 1", rs -> rs.next() ? rs.getLong(1) : null);
        }

        OffsetDateTime now = OffsetDateTime.now(CLINIC_ZONE);
        Long apptId = insert("INSERT INTO appointments(patient_id, doctor_id, starts_at, ends_at, visit_type, status, reason, booked_by) VALUES(?,?,?,?, 'WALK_IN', 'COMPLETED', 'Clinical consultation', ?)",
                patientDbId, doctorDbId, now, now.plusMinutes(20), staff);

        String diagnosis = String.valueOf(body.getOrDefault("diagnosis", "Clinical Consultation"));
        String prescription = String.valueOf(body.getOrDefault("prescription", ""));
        String labTests = String.valueOf(body.getOrDefault("labTests", ""));
        String notes = String.valueOf(body.getOrDefault("notes", ""));

        Long recordId = insert("INSERT INTO encounters(appointment_id, patient_id, doctor_id, complaint, clinical_notes, current_medicines, diagnosis_text, status, created_by, finalized_at) VALUES(?,?,?,?,?,?,?, 'FINALIZED', ?, CURRENT_TIMESTAMP)",
                apptId, patientDbId, doctorDbId, labTests, notes, prescription, diagnosis, staff);

        return Map.of("message", "Electronic health record saved successfully", "recordId", recordId);
    }

    @DeleteMapping("/records")
    @Transactional
    public Map<String, Object> deleteRecord(@RequestParam long id) {
        db.update("DELETE FROM invoice_items WHERE invoice_id IN (SELECT id FROM invoices WHERE encounter_id = ?)", id);
        db.update("DELETE FROM payments WHERE invoice_id IN (SELECT id FROM invoices WHERE encounter_id = ?)", id);
        db.update("DELETE FROM invoices WHERE encounter_id = ?", id);
        db.update("DELETE FROM dispensings WHERE prescription_item_id IN (SELECT pi.id FROM prescription_items pi JOIN prescriptions p ON p.id = pi.prescription_id WHERE p.encounter_id = ?)", id);
        db.update("DELETE FROM prescription_items WHERE prescription_id IN (SELECT id FROM prescriptions WHERE encounter_id = ?)", id);
        db.update("DELETE FROM prescriptions WHERE encounter_id = ?", id);
        db.update("DELETE FROM encounter_addenda WHERE encounter_id = ?", id);
        int deleted = db.update("DELETE FROM encounters WHERE id = ?", id);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
        }
        return Map.of("message", "EHR record removed successfully", "id", id);
    }

    private Long insert(String sql, Object... params) {
        org.springframework.jdbc.support.KeyHolder keys = new org.springframework.jdbc.support.GeneratedKeyHolder();
        db.update(con -> {
            java.sql.PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keys);
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("Insert did not generate a database key");
        }
        return key.longValue();
    }
}
