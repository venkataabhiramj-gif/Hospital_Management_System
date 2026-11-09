package com.hospital.config;

import com.hospital.exceptions.DatabaseException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Enterprise Database Configuration & JDBC Connection Manager.
 * Implements thread-safe Singleton pattern with automated schema migration
 * and clinical seed records for the outpatient EHR system.
 */
public class DatabaseConfig {
    private static DatabaseConfig instance;
    private static final String DB_FILE = "hospital.db";
    private static final String SQLITE_URL = "jdbc:sqlite:" + DB_FILE;

    private DatabaseConfig() {
        try {
            Class.forName("org.sqlite.JDBC");
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            System.err.println("[CRITICAL] SQLite JDBC Driver not found in classpath. Check lib/sqlite-jdbc.jar.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Database initialization failed: " + e.getMessage());
        }
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    public Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(SQLITE_URL);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to establish JDBC connection to database engine.", e);
        }
    }

    private void initializeDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(SQLITE_URL);
             Statement stmt = conn.createStatement()) {

            // 1. Patient Master Index
            stmt.execute("CREATE TABLE IF NOT EXISTS patients (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT, " +
                    "phone TEXT NOT NULL, " +
                    "age INTEGER NOT NULL, " +
                    "gender TEXT NOT NULL, " +
                    "blood_group TEXT, " +
                    "address TEXT, " +
                    "emergency_contact TEXT, " +
                    "registered_date TEXT NOT NULL);");

            // 2. Clinicians & Specialist Directory
            stmt.execute("CREATE TABLE IF NOT EXISTS doctors (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT, " +
                    "phone TEXT NOT NULL, " +
                    "age INTEGER NOT NULL, " +
                    "gender TEXT NOT NULL, " +
                    "specialization TEXT NOT NULL, " +
                    "qualification TEXT NOT NULL, " +
                    "consultation_fee REAL NOT NULL, " +
                    "available_days TEXT, " +
                    "available_hours TEXT);");

            // 3. Clinical Encounters & Appointments
            stmt.execute("CREATE TABLE IF NOT EXISTS appointments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "patient_id TEXT NOT NULL, " +
                    "patient_name TEXT NOT NULL, " +
                    "doctor_id TEXT NOT NULL, " +
                    "doctor_name TEXT NOT NULL, " +
                    "appointment_date TEXT NOT NULL, " +
                    "time_slot TEXT NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "reason TEXT, " +
                    "FOREIGN KEY (patient_id) REFERENCES patients(id), " +
                    "FOREIGN KEY (doctor_id) REFERENCES doctors(id));");

            // 4. Inpatient & Outpatient Billing (Revenue Cycle Management)
            stmt.execute("CREATE TABLE IF NOT EXISTS bills (" +
                    "bill_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "patient_id TEXT NOT NULL, " +
                    "patient_name TEXT NOT NULL, " +
                    "consultation_fee REAL NOT NULL, " +
                    "treatment_charges REAL NOT NULL, " +
                    "medicine_charges REAL NOT NULL, " +
                    "discount_percent REAL NOT NULL, " +
                    "tax_percent REAL NOT NULL, " +
                    "total_amount REAL NOT NULL, " +
                    "payment_status TEXT NOT NULL, " +
                    "billing_date TEXT NOT NULL, " +
                    "FOREIGN KEY (patient_id) REFERENCES patients(id));");

            // 5. Electronic Health Records (EHR) & Clinical Notes
            stmt.execute("CREATE TABLE IF NOT EXISTS medical_records (" +
                    "record_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "patient_id TEXT NOT NULL, " +
                    "patient_name TEXT NOT NULL, " +
                    "doctor_id TEXT NOT NULL, " +
                    "doctor_name TEXT NOT NULL, " +
                    "diagnosis TEXT NOT NULL, " +
                    "prescription TEXT NOT NULL, " +
                    "lab_tests TEXT, " +
                    "notes TEXT, " +
                    "record_date TEXT NOT NULL, " +
                    "FOREIGN KEY (patient_id) REFERENCES patients(id), " +
                    "FOREIGN KEY (doctor_id) REFERENCES doctors(id));");

            seedInitialData(stmt);
        }
    }

    private void seedInitialData(Statement stmt) throws SQLException {
        var rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM doctors");
        if (rs.next() && rs.getInt("cnt") == 0) {
            // Seed Professional Attending Doctors
            stmt.execute("INSERT INTO doctors VALUES " +
                    "('DOC-401', 'Dr. Ramesh Sharma, MD', 'r.sharma@apexcare.org', '+91 98450 11220', 48, 'Male', 'Interventional Cardiology', 'MBBS, MD (Medicine), DM (Cardiology), FACC', 1000.0, 'Mon, Wed, Fri', '09:00 AM - 01:00 PM')," +
                    "('DOC-402', 'Dr. Priya Sundaram, DM', 'p.sundaram@apexcare.org', '+91 98450 11221', 41, 'Female', 'Neurology & Stroke Care', 'MBBS, MD, DM (Neurology)', 1200.0, 'Tue, Thu, Sat', '10:00 AM - 02:00 PM')," +
                    "('DOC-403', 'Dr. Arvind K. Verma, MS', 'a.verma@apexcare.org', '+91 98450 11222', 45, 'Male', 'Orthopedic Surgery & Sports Med', 'MBBS, MS (Orthopedics), MCh (Joint Repl)', 900.0, 'Mon, Tue, Thu', '02:00 PM - 06:00 PM')," +
                    "('DOC-404', 'Dr. Shalini Mukhopadhyay, DNB', 's.mukho@apexcare.org', '+91 98450 11223', 39, 'Female', 'Pediatrics & Neonatology', 'MBBS, DCH, DNB (Pediatrics)', 750.0, 'Mon, Wed, Sat', '09:00 AM - 01:00 PM')," +
                    "('DOC-405', 'Dr. Rajesh Nair, MD', 'r.nair@apexcare.org', '+91 98450 11224', 52, 'Male', 'Internal Medicine & Diabetology', 'MBBS, MD (General Medicine), FRCP', 850.0, 'Mon to Fri', '08:30 AM - 12:30 PM');");

            // Seed Realistic Outpatient Profiles (with MRN style IDs)
            stmt.execute("INSERT INTO patients VALUES " +
                    "('MRN-84210', 'Ananya Deshmukh', 'ananya.deshmukh@gmail.com', '+91 91234 56780', 29, 'Female', 'O+', 'Flat 402, Oakwood Enclave, Vijayawada', '+91 98450 99911 (Spouse)', '2026-11-01')," +
                    "('MRN-84211', 'Rahul Srinivasan', 'rahul.s@outlook.com', '+91 91234 56781', 42, 'Male', 'B+', 'Villa 12, Palm Meadows, Amaravati', '+91 98450 99922 (Brother)', '2026-11-03')," +
                    "('MRN-84212', 'Sunita Ramachandran', 'sunita.r@tcs.com', '+91 91234 56782', 63, 'Female', 'A+', 'Plot 88, Sector 4, Guntur', '+91 98450 99933 (Son)', '2026-11-04')," +
                    "('MRN-84213', 'Mohammed Zeeshan', 'zeeshan.m@gmail.com', '+91 91234 56783', 35, 'Male', 'AB+', 'Door 3-14, Auto Nagar, Vijayawada', '+91 98450 99944 (Self)', '2026-11-05');");

            // Seed Active Scheduled Clinical Encounters
            stmt.execute("INSERT INTO appointments (patient_id, patient_name, doctor_id, doctor_name, appointment_date, time_slot, status, reason) VALUES " +
                    "('MRN-84210', 'Ananya Deshmukh', 'DOC-401', 'Dr. Ramesh Sharma, MD', '2026-11-12', '10:00 AM - 10:30 AM', 'BOOKED', 'Post-COVID palpitation review & lipid profile follow-up')," +
                    "('MRN-84211', 'Rahul Srinivasan', 'DOC-402', 'Dr. Priya Sundaram, DM', '2026-11-12', '11:00 AM - 11:30 AM', 'BOOKED', 'Chronic migraine headache with aura evaluation')," +
                    "('MRN-84212', 'Sunita Ramachandran', 'DOC-405', 'Dr. Rajesh Nair, MD', '2026-11-10', '09:00 AM - 09:30 AM', 'COMPLETED', 'Type 2 Diabetes quarterly HbA1c review & BP titration')," +
                    "('MRN-84213', 'Mohammed Zeeshan', 'DOC-403', 'Dr. Arvind K. Verma, MS', '2026-11-08', '03:00 PM - 03:30 PM', 'COMPLETED', 'Right knee lateral meniscus strain & rehabilitation plan');");

            // Seed Commercial Medical Invoices (GST & RCM breakdown)
            stmt.execute("INSERT INTO bills (patient_id, patient_name, consultation_fee, treatment_charges, medicine_charges, discount_percent, tax_percent, total_amount, payment_status, billing_date) VALUES " +
                    "('MRN-84212', 'Sunita Ramachandran', 850.0, 600.0, 1420.0, 5.0, 5.0, 2862.65, 'PAID', '2026-11-08')," +
                    "('MRN-84213', 'Mohammed Zeeshan', 900.0, 1500.0, 450.0, 0.0, 5.0, 2992.50, 'PAID', '2026-11-08')," +
                    "('MRN-84210', 'Ananya Deshmukh', 1000.0, 1200.0, 850.0, 10.0, 5.0, 2882.25, 'PENDING', '2026-11-09');");

            // Seed Comprehensive EHR Medical Records
            stmt.execute("INSERT INTO medical_records (patient_id, patient_name, doctor_id, doctor_name, diagnosis, prescription, lab_tests, notes, record_date) VALUES " +
                    "('MRN-84212', 'Sunita Ramachandran', 'DOC-405', 'Dr. Rajesh Nair, MD', " +
                    "'Type 2 Diabetes Mellitus (E11.9) with Essential Stage 1 Hypertension (I10)', " +
                    "'1. Tab. Metformin 500mg PO BID with meals\n2. Tab. Telmisartan 40mg PO OD morning\n3. Tab. Atorvastatin 10mg PO HS', " +
                    "'HbA1c: 7.2% | Fasting Glucose: 134 mg/dL | Serum Creatinine: 0.9 mg/dL', " +
                    "'BP recorded at 134/86 mmHg, HR 74 bpm regular. Advised diabetic diet counseling, low glycemic index foods, 30 min daily brisk walking. Follow up in 90 days with repeat lipid panel.', '2026-11-08')," +
                    "('MRN-84213', 'Mohammed Zeeshan', 'DOC-403', 'Dr. Arvind K. Verma, MS', " +
                    "'Right Knee Medial Collateral Ligament (MCL) Grade 1 Sprain (S83.4)', " +
                    "'1. Tab. Aceclofenac 100mg + Paracetamol 325mg PO BD x 5 days\n2. Tab. Pantoprazole 40mg PO OD AC\n3. Topical Diclofenac gel TID application', " +
                    "'Knee X-Ray AP/Lateral: No fracture or joint space narrowing. Stable ligamentous testing.', " +
                    "'Mild joint effusion noted. Prescribed hinged knee brace support for 2 weeks. R.I.C.E protocol initiated. Avoid contact sports and stair climbing for 3 weeks. Review in Ortho OPD after 14 days.', '2026-11-08');");

            System.out.println("[DB] Enterprise schema verified. Seeded clinical registry & encounter data.");
        }
    }
}
