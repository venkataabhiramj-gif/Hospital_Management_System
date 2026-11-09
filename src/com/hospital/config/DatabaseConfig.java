package com.hospital.config;

import com.hospital.exceptions.DatabaseException;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database configuration and connection manager.
 * Demonstrates:
 *  1. Singleton Pattern (single shared database connection manager)
 *  2. Safe JDBC connection handling
 *  3. Automated DDL schema execution & initial sample data seeding
 */
public class DatabaseConfig {
    private static DatabaseConfig instance;
    private static final String DB_FILE = "hospital.db";
    private static final String SQLITE_URL = "jdbc:sqlite:" + DB_FILE;
    
    // Private constructor prevents direct instantiation (Singleton pattern)
    private DatabaseConfig() {
        try {
            // Load SQLite JDBC Driver class dynamically
            Class.forName("org.sqlite.JDBC");
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL: SQLite JDBC Driver not found in classpath! Check lib/ folder.");
        } catch (SQLException e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
        }
    }

    /**
     * Thread-safe Singleton instance retrieval.
     */
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    /**
     * Obtains a new JDBC Connection.
     */
    public Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(SQLITE_URL);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to establish database connection.", e);
        }
    }

    /**
     * Initializes the relational database schema if tables do not exist.
     * Uses standard SQL compatible with both SQLite and relational engines.
     */
    private void initializeDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(SQLITE_URL);
             Statement stmt = conn.createStatement()) {

            // 1. Patients Table
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

            // 2. Doctors Table
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

            // 3. Appointments Table
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

            // 4. Bills Table
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

            // 5. Medical Records Table
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

    /**
     * Seeds initial records on first run so the system has demonstrable data during demo/viva.
     */
    private void seedInitialData(Statement stmt) throws SQLException {
        // Check if doctors table has data
        var rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM doctors");
        if (rs.next() && rs.getInt("cnt") == 0) {
            // Seed Doctors
            stmt.execute("INSERT INTO doctors VALUES " +
                    "('DOC-101', 'Dr. Ramesh Sharma', 'ramesh.sharma@hospital.com', '9876543210', 45, 'Male', 'Cardiology', 'MBBS, MD (Cardio)', 800.0, 'Mon, Wed, Fri', '09:00 AM - 01:00 PM')," +
                    "('DOC-102', 'Dr. Priya Patel', 'priya.patel@hospital.com', '9876543211', 38, 'Female', 'Neurology', 'MBBS, DM (Neuro)', 950.0, 'Tue, Thu, Sat', '10:00 AM - 02:00 PM')," +
                    "('DOC-103', 'Dr. Arvind Verma', 'arvind.verma@hospital.com', '9876543212', 42, 'Male', 'Orthopedics', 'MBBS, MS (Ortho)', 700.0, 'Mon, Tue, Thu', '02:00 PM - 06:00 PM');");

            // Seed Patients
            stmt.execute("INSERT INTO patients VALUES " +
                    "('PAT-1001', 'Ananya Deshmukh', 'ananya.d@gmail.com', '9123456780', 28, 'Female', 'O+', 'Plot 42, Sector 5, Amaravati', '9123456789', '2026-11-01')," +
                    "('PAT-1002', 'Rahul Srinivasan', 'rahul.s@outlook.com', '9123456781', 35, 'Male', 'B+', 'Flat 302, Green Meadows, Vijayawada', '9123456788', '2026-11-03');");

            // Seed Appointment
            stmt.execute("INSERT INTO appointments (patient_id, patient_name, doctor_id, doctor_name, appointment_date, time_slot, status, reason) VALUES " +
                    "('PAT-1001', 'Ananya Deshmukh', 'DOC-101', 'Dr. Ramesh Sharma', '2026-11-12', '10:00 AM - 10:30 AM', 'BOOKED', 'Routine cardiology consultation and BP check');");

            // Seed Bill
            stmt.execute("INSERT INTO bills (patient_id, patient_name, consultation_fee, treatment_charges, medicine_charges, discount_percent, tax_percent, total_amount, payment_status, billing_date) VALUES " +
                    "('PAT-1001', 'Ananya Deshmukh', 800.0, 350.0, 450.0, 5.0, 5.0, 1596.0, 'PAID', '2026-11-05');");

            // Seed Medical Record
            stmt.execute("INSERT INTO medical_records (patient_id, patient_name, doctor_id, doctor_name, diagnosis, prescription, lab_tests, notes, record_date) VALUES " +
                    "('PAT-1001', 'Ananya Deshmukh', 'DOC-101', 'Dr. Ramesh Sharma', 'Mild hypertension and elevated fatigue', 'Tab. Telmisartan 40mg once daily; Multivitamin', 'Lipid Profile, Serum Electrolytes', 'Advised 30 min daily brisk walking and low sodium intake.', '2026-11-05');");
            
            System.out.println("Initialized database schema and seeded demonstration data successfully.");
        }
    }
}
