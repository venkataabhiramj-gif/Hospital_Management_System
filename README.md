# 🏥 ApexCare Clinical OS & Healthcare Information System (HIS)
### Outpatient EHR, Clinical Scheduling, Revenue Cycle Management & Multi-Platform Portal

ApexCare is a multi-platform Healthcare Information System built entirely using **Java (JDK 17)**. It connects a centralized Spring Boot REST API and relational database with three unified clients:
- **🖥️ Desktop Application (`:desktop`)**: Windows desktop client (`ClinicDesk`) with role-aware navigation, in-app stock & expiry alerts, and dashboard snapshots.
- **📱 Android Application (`:android`)**: Native Android mobile client for point-of-care clinical management.
- **🌐 Web Portal (`webapp/`)**: Responsive clinical web interface for outpatient registration, appointment booking, clinical encounters, billing, and EHR management.

All three platforms connect to the **same centralized database** (H2 for local development, PostgreSQL for production), ensuring any change made in one application is instantly reflected across desktop, mobile, and web.

---

## 🌟 Clinical Capabilities & Subsystems

| Subsystem | Capabilities & Clinical Workflow |
| :--- | :--- |
| **🧑‍⚕️ Master Patient Index (MPI)** | Centralized outpatient registration, unique Medical Record Numbers (MRN), demographic intake, emergency contact kin-mapping, and real-time query filtering. |
| **👨‍⚕️ Clinician Staff Directory** | Medical credentials tracking, clinical department appointments, OPD consultation fee scheduling, and shift availability windows. |
| **📅 Outpatient Scheduling & Encounters** | Multi-specialty booking engine with **transactional double-booking conflict prevention**, check-in status, and encounter completion tracking. |
| **💳 Revenue Cycle Management (RCM)** | Point-of-care billing engine implementing formal financial contracts, dynamic multi-tier discount application, statutory healthcare tax (GST) auditing, and printable tax receipts. |
| **📋 Electronic Health Records (EHR)** | Clinical progress notes, ICD-10 indication tracking, structured prescription dosing regimens (Rx), diagnostic lab/radiology orders, and longitudinal chart reviews. |

---

## 🚀 Execution Instructions

### 1. Start the Centralized Server (`:server`)
Set an initial administrator password (min 12 characters) and run the Spring Boot API service:
```powershell
$env:HMS_ADMIN_PASSWORD = 'choose-a-unique-password-of-at-least-12-characters'
.\gradlew.bat :server:bootRun
```
*The server boots on `http://localhost:8080`, initializes the database migrations (`data/hospital.mv.db`), and serves the REST API and the integrated web portal at `http://localhost:8080/`.*

### 2. Start the Windows Desktop App (`:desktop`)
In a separate terminal:
```powershell
.\gradlew.bat :desktop:run
```

To create a portable Windows package with its own Java runtime:
```powershell
.\gradlew.bat :desktop:packageWindowsApp
```

### 3. Open the Web Portal
With the server running, navigate to:
```
http://localhost:8080/
```
The web portal communicates directly with the unified `:server` API using the same database.

### 4. Android Client (`:android`)
The native Android project is located under `android/`. It connects to the unified API at `http://10.0.2.2:8080/api/` in Android Emulator or your server's LAN IP on physical devices.

---

## 📁 Repository Directory Structure

```
Hospital_Management_System/
├── server/                                  # Spring Boot REST API & Central Database
│   ├── src/main/java/com/hospital/management/server/
│   │   ├── api/WorkflowController.java      # Unified REST API endpoints
│   │   ├── config/SecurityConfiguration.java# Spring Security & Web asset configuration
│   │   └── service/                         # Clinical business logic & duplicate review
│   └── src/main/resources/
│       ├── application.yml                  # H2 Database configuration (./data/hospital)
│       └── db/migration/                    # Flyway migrations (V1 to V6)
├── desktop/                                 # Windows JavaFX Desktop Application (ClinicDesk)
│   └── src/main/java/com/hospital/management/desktop/
├── android/                                 # Native Android Application
│   └── app/src/main/java/com/hospital/management/android/
├── webapp/                                  # Responsive Web Interface (HTML5/CSS3/Vanilla JS)
│   ├── index.html                           # Operations Dashboard
│   ├── patients.html                        # Master Patient Index (MPI)
│   ├── doctors.html                         # Medical Staff Roster
│   ├── appointments.html                   # Outpatient Scheduling & Check-in
│   ├── billing.html                         # RCM & Point-of-Care Invoicing
│   ├── records.html                         # Electronic Health Records (EHR)
│   ├── css/style.css                        # Design System & Clinical Tokens
│   └── js/app.js                            # Frontend API bridge to unified server
├── docs/                                    # Documentation & Guides
│   ├── ASSIGNMENT_DEMO.md
│   ├── CODE_EXPLANATION.md                  # Comprehensive line-by-line explanation
│   ├── DEMO_DATA.md
│   └── OPERATIONS.md
├── build.gradle                             # Root Gradle configuration
├── settings.gradle
└── compose.yaml                             # Docker Compose for PostgreSQL & Server
```
