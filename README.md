# 🏥 Hospital Management System (HMS) — Java Web Application

A full-featured outpatient **Hospital Management System** built entirely using **Java (JDK 17)** with an embedded web server, **JDBC relational persistence**, and a responsive healthcare portal frontend.

Developed as a guided academic assignment and mini-project for **2nd Year 1st Semester CSE (SRM University AP)** covering core Java concepts: Object-Oriented Programming (OOP), Interfaces, Custom Exception Handling, and Database Connectivity.

---

## 🌟 Key Features & Modules

| Module | Features & Capabilities |
| :--- | :--- |
| **🧑‍🤝‍🧑 Patient Registration** | Register outpatients, validate age/contact, blood group tracking, live instant search by name, phone, or ID. |
| **👨‍⚕️ Doctor Management** | Add/edit doctor profiles, qualifications, consulting fees, schedules, and filter by specialization. |
| **📅 Appointment Booking** | Doctor appointment scheduling, time slot assignment, and **automated conflict detection** (preventing double booking). |
| **💳 Billing & Invoices** | Financial calculation engine implementing `Billable` interface, automated discount and GST calculations, live preview, invoice generation & payment tracking. |
| **📋 Medical Records** | Clinical encounter logs, diagnoses, prescriptions, lab investigations, and doctor's clinical notes. |

---

## 🎯 Java Academic Concepts Demonstrated

1. **Object-Oriented Programming (OOP)**:
   - **Abstraction:** Abstract base class `Person` defining shared attributes and abstract `getRole()` contract.
   - **Encapsulation:** Private attributes with validation inside setters (e.g., age validation $0 \le \text{age} \le 130$).
   - **Inheritance:** `Patient extends Person` and `Doctor extends Person` reusing foundational state and behavior.
   - **Polymorphism:** Runtime polymorphism via method overriding (`getRole()`, `toString()`) and compile-time polymorphism via constructor/method overloading.
2. **Interfaces & Decoupling**:
   - `Billable`: Financial contract for tax, discount, subtotal, and total amount computation.
   - DAO Interfaces (`PatientDAO`, `DoctorDAO`, `AppointmentDAO`, `BillingDAO`, `MedicalRecordDAO`): Separation of business rules from data storage.
3. **Custom Exception Hierarchy**:
   - `HospitalException` (Base checked exception)
   - `AppointmentConflictException` (Doctor slot conflict)
   - `InvalidBillingException` (Financial rule validation)
   - `PatientNotFoundException` & `DoctorNotFoundException`
   - `DatabaseException` (JDBC exception encapsulation)
4. **Database Connectivity (JDBC)**:
   - **Zero SQL Injection:** 100% parameterized queries via `PreparedStatement`.
   - **Resource Safety:** Automatic cleanup via `try-with-resources` (`Connection`, `PreparedStatement`, `ResultSet`).
   - **Thread-Safe Singleton:** `DatabaseConfig` manages database initialization and automatic table schema creation.

---

## 🚀 How to Run the Project (Windows / Mac / Linux)

### Option 1: Quick Run on Windows (Double-Click)
1. Double-click `run.bat` (it automatically compiles if needed and launches the server).
2. Open your web browser and navigate to:
   ```
   http://localhost:8088
   ```

### Option 2: Command Line (Any OS)
1. **Compile**:
   ```bash
   javac -cp "lib/*" -d bin src/com/hospital/**/*.java src/com/hospital/*.java
   ```
2. **Run**:
   ```bash
   java -cp "bin;lib/*" com.hospital.Main 8088
   ```
   *(On macOS / Linux, use colon separator: `java -cp "bin:lib/*" com.hospital.Main 8088`)*
3. **Open in Browser**:
   ```
   http://localhost:8088
   ```

---

## 📁 Project Architecture & Directory Structure

```
Hospital_Management_System/
├── src/
│   └── com/hospital/
│       ├── Main.java                        # Server Entry Point (http://localhost:8088)
│       ├── config/
│       │   └── DatabaseConfig.java          # JDBC Connection Manager (Singleton Pattern)
│       ├── exceptions/                      # Custom Exceptions Hierarchy
│       │   ├── HospitalException.java       # Base Checked Exception
│       │   ├── PatientNotFoundException.java
│       │   ├── DoctorNotFoundException.java
│       │   ├── AppointmentConflictException.java
│       │   ├── InvalidBillingException.java
│       │   └── DatabaseException.java
│       ├── models/                          # Core OOP Domain Entities
│       │   ├── Person.java                  # Abstract Base Class (Abstraction & Encapsulation)
│       │   ├── Patient.java                 # Subclass of Person (Inheritance)
│       │   ├── Doctor.java                  # Subclass of Person (Inheritance)
│       │   ├── Appointment.java             # Entity with scheduling state
│       │   ├── Bill.java                    # Implements Billable interface
│       │   └── MedicalRecord.java           # Clinical consultations & history
│       ├── interfaces/                      # Formal Interface Contracts
│       │   ├── Billable.java                # Financial calculation interface
│       │   ├── PatientDAO.java              # Patient CRUD contract
│       │   ├── DoctorDAO.java               # Doctor CRUD contract
│       │   ├── AppointmentDAO.java          # Scheduling contract
│       │   ├── BillingDAO.java              # Invoicing contract
│       │   └── MedicalRecordDAO.java        # Health record contract
│       ├── dao/                             # Safe Parameterized JDBC Implementations
│       │   ├── PatientDAOImpl.java
│       │   ├── DoctorDAOImpl.java
│       │   ├── AppointmentDAOImpl.java
│       │   ├── BillingDAOImpl.java
│       │   └── MedicalRecordDAOImpl.java
│       └── web/                             # Web Server & Controllers
│           ├── ApiHandler.java              # REST API & Form Submission Controller
│           ├── JsonUtils.java               # Lightweight Pure-Java JSON formatter
│           └── StaticFileHandler.java       # Static Web Asset Server
├── webapp/                                  # Responsive Web UI
│   ├── index.html                           # Operations Dashboard
│   ├── patients.html                        # Patient Registration & Directory
│   ├── doctors.html                         # Doctor Management
│   ├── appointments.html                   # Appointment Scheduling & Slot Conflict Check
│   ├── billing.html                         # Invoicing & Payment Tracker
│   ├── records.html                         # Clinical Consultation Logs
│   ├── css/
│   │   └── style.css                        # Modern Clean Healthcare Design
│   └── js/
│       └── app.js                           # Frontend controller & API bridge
├── lib/
│   └── sqlite-jdbc.jar                      # Self-contained JDBC Driver
├── docs/
│   ├── CODE_EXPLANATION.md                  # Comprehensive line-by-line guide for Lab Viva
│   └── PROJECT_BRIEF.md                     # Outpatient hospital system workflow brief
├── build.bat                                # Windows build script
└── run.bat                                  # Windows launch script
```

---

## 📖 Comprehensive Viva Preparation Guide
For a complete line-by-line explanation of every class, method, design decision, and typical lab viva questions with model answers, see [docs/CODE_EXPLANATION.md](docs/CODE_EXPLANATION.md).
