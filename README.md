# 🏥 ApexCare Clinical OS & Healthcare Information System (HIS)
### Enterprise Outpatient EHR, Clinical Scheduling & Revenue Cycle Management

ApexCare is a production-oriented, full-stack outpatient Healthcare Information System built entirely using **Java (JDK 17)** with an embedded concurrent HTTP server, **secure JDBC relational persistence**, and a responsive clinical web interface.

---

## 🌟 Clinical Capabilities & Subsystems

| Subsystem | Capabilities & Clinical Workflow |
| :--- | :--- |
| **🧑‍⚕️ Master Patient Index (MPI)** | Centralized outpatient registration, unique Medical Record Numbers (MRN), demographic intake, emergency contact kin-mapping, and real-time query filtering. |
| **👨‍⚕️ Clinician Staff Directory** | Medical credentials tracking, clinical department appointments, OPD consultation fee scheduling, and shift availability windows. |
| **📅 Outpatient Scheduling & Encounters** | Multi-specialty booking engine with **transactional double-booking conflict prevention** (`AppointmentConflictException`), check-in status, and encounter completion tracking. |
| **💳 Revenue Cycle Management (RCM)** | Point-of-care billing engine implementing the formal `Billable` financial contract, dynamic multi-tier discount application, statutory healthcare tax (GST) auditing, and printable tax receipts. |
| **📋 Electronic Health Records (EHR)** | Clinical progress notes, ICD-10 indication tracking, structured prescription dosing regimens (Rx), diagnostic lab/radiology orders, and longitudinal chart reviews. |

---

## 🏛️ Java Engineering Architecture & OOP Principles

1. **Object-Oriented Programming (OOP) Hierarchy**:
   - **Abstraction:** The abstract base class `Person` encapsulates standard demographic traits (`id`, `name`, `phone`, `email`, `age`, `gender`) and enforces the polymorphic contract `public abstract String getRole()`.
   - **Encapsulation:** Private attributes guarded by strict validation rules within mutator methods (e.g., age boundaries $0 \le \text{age} \le 130$, non-null strings).
   - **Inheritance:** `Patient extends Person` and `Doctor extends Person` derive foundational state while encapsulating department-specific clinical attributes.
   - **Polymorphism:** Runtime polymorphism via method overriding (`getRole()`, `toString()`) and compile-time polymorphism via constructor and query method overloading.
2. **Interface Abstraction & Decoupling**:
   - `Billable`: Interface defining financial calculation contracts (`calculateSubtotal`, `calculateDiscountAmount`, `calculateTaxAmount`, `calculateFinalTotal`).
   - Data Access Object (DAO) Contracts (`PatientDAO`, `DoctorDAO`, `AppointmentDAO`, `BillingDAO`, `MedicalRecordDAO`): Complete isolation between business rules and data storage engines.
3. **Domain Exception Handling**:
   - Strict custom exception hierarchy extending `HospitalException`:
     - `AppointmentConflictException` (Physician schedule collision $\rightarrow$ HTTP 409 Conflict)
     - `InvalidBillingException` (Financial computation anomaly $\rightarrow$ HTTP 400 Bad Request)
     - `PatientNotFoundException` & `DoctorNotFoundException` (Entity query miss $\rightarrow$ HTTP 404 Not Found)
     - `DatabaseException` (JDBC encapsulation $\rightarrow$ HTTP 500 Internal Error)
4. **Relational Database Connectivity (JDBC)**:
   - **100% Parameterized Queries:** Universal execution via `PreparedStatement` eliminating SQL injection vectors.
   - **Deterministic Resource Management:** Strict `try-with-resources` ensuring zero connection, statement, or cursor leaks.
   - **Thread-Safe Singleton Manager:** `DatabaseConfig` manages database initialization and automatic table schema creation.

---

## 🚀 Execution Instructions

### Windows (One-Click)
1. Double-click `run.bat` (automatically compiles source files if needed and boots the HTTP server).
2. Open your web browser and navigate to:
   ```
   http://localhost:8088
   ```

### Cross-Platform CLI (Windows / Linux / macOS)
1. **Compile**:
   ```bash
   javac -cp "lib/*" -d bin src/com/hospital/**/*.java src/com/hospital/*.java
   ```
2. **Launch**:
   ```bash
   java -cp "bin;lib/*" com.hospital.Main 8088
   ```
   *(On macOS / Linux, use colon delimiter: `java -cp "bin:lib/*" com.hospital.Main 8088`)*
3. **Access**:
   ```
   http://localhost:8088
   ```

---

## 📁 Repository Directory Structure

```
Hospital_Management_System/
├── src/
│   └── com/hospital/
│       ├── Main.java                        # Server Entry Point (http://localhost:8088)
│       ├── config/
│       │   └── DatabaseConfig.java          # JDBC Connection Manager (Singleton Pattern)
│       ├── exceptions/                      # Custom Domain Exception Hierarchy
│       │   ├── HospitalException.java       # Base Checked Exception
│       │   ├── PatientNotFoundException.java
│       │   ├── DoctorNotFoundException.java
│       │   ├── AppointmentConflictException.java
│       │   ├── InvalidBillingException.java
│       │   └── DatabaseException.java
│       ├── models/                          # Core Domain Entities (OOP)
│       │   ├── Person.java                  # Abstract Base Class (Abstraction & Encapsulation)
│       │   ├── Patient.java                 # Subclass of Person (Inheritance)
│       │   ├── Doctor.java                  # Subclass of Person (Inheritance)
│       │   ├── Appointment.java             # Entity with scheduling state
│       │   ├── Bill.java                    # Implements Billable interface
│       │   └── MedicalRecord.java           # Longitudinal EHR record
│       ├── interfaces/                      # Formal Interface Contracts
│       │   ├── Billable.java                # Financial calculation interface
│       │   ├── PatientDAO.java              # Patient CRUD contract
│       │   ├── DoctorDAO.java               # Doctor CRUD contract
│       │   ├── AppointmentDAO.java          # Scheduling contract
│       │   ├── BillingDAO.java              # Invoicing contract
│       │   └── MedicalRecordDAO.java        # Health record contract
│       ├── dao/                             # Parameterized JDBC Implementations
│       │   ├── PatientDAOImpl.java
│       │   ├── DoctorDAOImpl.java
│       │   ├── AppointmentDAOImpl.java
│       │   ├── BillingDAOImpl.java
│       │   └── MedicalRecordDAOImpl.java
│       └── web/                             # Web Server & Controllers
│           ├── ApiHandler.java              # REST API & Form Submission Controller
│           ├── JsonUtils.java               # Pure-Java JSON serialization utility
│           └── StaticFileHandler.java       # Static Web Asset Server
├── webapp/                                  # Responsive Healthcare UI
│   ├── index.html                           # Operations Dashboard
│   ├── patients.html                        # Master Patient Index (MPI)
│   ├── doctors.html                         # Medical Staff Roster
│   ├── appointments.html                   # Outpatient Scheduling & Check-in
│   ├── billing.html                         # RCM & Point-of-Care Invoicing
│   ├── records.html                         # Electronic Health Records (EHR)
│   ├── css/
│   │   └── style.css                        # Design System & Clinical Tokens
│   └── js/
│       └── app.js                           # Frontend controller & API bridge
├── lib/
│   └── sqlite-jdbc.jar                      # Embedded JDBC Driver (Auto-downloaded on build)
├── docs/
│   ├── CODE_EXPLANATION.md                  # Comprehensive line-by-line code & architecture guide
│   └── PROJECT_BRIEF.md                     # Outpatient system product brief
├── build.bat                                # Windows build script
└── run.bat                                  # Windows launch script
```
