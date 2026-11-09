package com.hospital.management.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** Seeds clearly fictional records only when the explicitly enabled demo profile is active. */
@Component
@Profile("demo")
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String password;

    public DemoDataSeeder(JdbcTemplate jdbc, PasswordEncoder encoder,
                          @Value("${HMS_DEMO_STAFF_PASSWORD:}") String password) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Integer seeded = jdbc.queryForObject("SELECT COUNT(*) FROM patients WHERE patient_number='DEMO-IN-001'", Integer.class);
        if (seeded != null && seeded > 0) return;
        if (password == null || password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Set HMS_DEMO_STAFF_PASSWORD to a private password of 12–72 UTF-8 bytes before enabling the demo profile.");
        }

        long admin = adminId();
        long receptionist = createStaff("demo.reception", "Demo Reception Desk", "RECEPTIONIST");
        long doctorStaff = createStaff("demo.doctor", "Dr. Kavya Menon (Demo)", "DOCTOR");
        createStaff("demo.pharmacy", "Demo Pharmacy Desk", "PHARMACIST");
        createStaff("demo.billing", "Demo Billing Desk", "BILLING");
        long doctor = insert("INSERT INTO doctors(staff_user_id,specialty,appointment_minutes) VALUES(?,?,20)", doctorStaff, "General Medicine");
        for (int weekday = 1; weekday <= 7; weekday++) {
            jdbc.update("INSERT INTO doctor_schedule(doctor_id,day_of_week,starts_at,ends_at) VALUES(?,?,?,?)",
                    doctor, weekday, LocalTime.of(9, 0), LocalTime.of(17, 0));
        }

        List<DemoPatient> patients = List.of(
                new DemoPatient("DEMO-IN-001", "Ananya Rao", LocalDate.of(1994, 4, 12), "+91 00000 00001"),
                new DemoPatient("DEMO-IN-002", "Arjun Nair", LocalDate.of(1988, 9, 3), "+91 00000 00002"),
                new DemoPatient("DEMO-IN-003", "Meera Iyer", LocalDate.of(2001, 1, 26), "+91 00000 00003"),
                new DemoPatient("DEMO-IN-004", "Rohan Das", LocalDate.of(1979, 7, 8), "+91 00000 00004"),
                new DemoPatient("DEMO-IN-005", "Sana Qureshi", LocalDate.of(1997, 11, 19), "+91 00000 00005"),
                new DemoPatient("DEMO-IN-006", "Dev Patel", LocalDate.of(1968, 2, 14), "+91 00000 00006"));
        long[] patientIds = new long[patients.size()];
        for (int index = 0; index < patients.size(); index++) {
            DemoPatient patient = patients.get(index);
            patientIds[index] = insert("INSERT INTO patients(patient_number,full_name,date_of_birth,phone,created_by) VALUES(?,?,?,?,?)",
                    patient.number(), patient.name(), patient.dob(), patient.phone(), admin);
        }

        OffsetDateTime firstVisit = OffsetDateTime.now(CLINIC_ZONE).withSecond(0).withNano(0).plusMinutes(15);
        for (int index = 0; index < 4; index++) {
            OffsetDateTime start = firstVisit.plusMinutes(30L * index);
            String appointmentState = index == 1 ? "CHECKED_IN" : "BOOKED";
            jdbc.update("INSERT INTO appointments(patient_id,doctor_id,starts_at,ends_at,visit_type,status,reason,booked_by) VALUES(?,?,?,?,?,?,?,?)",
                    patientIds[index], doctor, start, start.plusMinutes(20), "APPOINTMENT", appointmentState, "Demonstration record — fictional", receptionist);
        }

        addMedicine(admin, "NLEM-PAR-500", "Paracetamol", "500 mg", "Tablet", "tablet", 10, 3, 2.50);
        addMedicine(admin, "NLEM-ORS", "Oral Rehydration Salts", "Standard sachet", "Powder sachet", "sachet", 15, 48, 9.00);
        addMedicine(admin, "NLEM-AMX-500", "Amoxicillin", "500 mg", "Capsule", "capsule", 20, 72, 4.00);
    }

    private long createStaff(String username, String name, String role) {
        Long existing = jdbc.query("SELECT id FROM staff_users WHERE username=?", rs -> rs.next() ? rs.getLong(1) : null, username);
        if (existing != null) return existing;
        return insert("INSERT INTO staff_users(username,password_hash,display_name,role) VALUES(?,?,?,?)",
                username, encoder.encode(password), name, role);
    }

    private long adminId() {
        Long id = jdbc.query("SELECT id FROM staff_users WHERE role='ADMIN' ORDER BY id", rs -> rs.next() ? rs.getLong(1) : null);
        if (id == null) throw new IllegalStateException("The initial admin account must exist before demo data is seeded.");
        return id;
    }

    private void addMedicine(long actor, String code, String name, String strength, String form, String unit,
                             int reorder, int quantity, double demoPrice) {
        long medicine = insert("INSERT INTO medicines(code,generic_name,strength,dosage_form,unit,reorder_level,sale_price) VALUES(?,?,?,?,?,?,?)",
                code, name, strength, form, unit, reorder, demoPrice);
        long batch = insert("INSERT INTO medicine_batches(medicine_id,batch_number,expiry_date,supplier,purchase_price,quantity_on_hand,received_by) VALUES(?,?,?,?,?,?,?)",
                medicine, "DEMO-" + code, LocalDate.now(CLINIC_ZONE).plusDays(180), "Fictional demo supplier", demoPrice * 0.6, quantity, actor);
        jdbc.update("INSERT INTO stock_movements(medicine_id,batch_id,movement_type,quantity_change,reason,reference_type,created_by) VALUES(?,?,'RECEIVED',?,'Fictional demonstration opening stock','DEMO',?)",
                medicine, batch, quantity, actor);
    }

    private long insert(String sql, Object... values) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            return statement;
        }, keys);
        java.util.Map<String, Object> generated = keys.getKeys();
        Object value = generated == null ? null : generated.entrySet().stream()
                .filter(entry -> "id".equalsIgnoreCase(entry.getKey()))
                .map(java.util.Map.Entry::getValue)
                .findFirst().orElse(null);
        if (!(value instanceof Number key)) throw new IllegalStateException("The database did not return a generated id.");
        return key.longValue();
    }

    private record DemoPatient(String number, String name, LocalDate dob, String phone) { }
}
