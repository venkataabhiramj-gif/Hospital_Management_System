# 🏥 ApexCare Clinical OS & Healthcare Information System (HIS)
### Multi-Platform Outpatient EHR, Clinical Scheduling, Revenue Cycle Management & Point-of-Care Portal

[![Java](https://img.shields.io/badge/Java-17%20LTS-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![JavaFX](https://img.shields.io/badge/JavaFX-17.0.8-FF8800?logo=oracle&logoColor=white)](https://openjfx.io/)
[![Android](https://img.shields.io/badge/Android-Native%20API-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Database](https://img.shields.io/badge/Database-PostgreSQL%20%7C%20H2-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Cloudflare Edge](https://img.shields.io/badge/Cloudflare-Global%20Tunnel-F38020?logo=cloudflare&logoColor=white)](https://www.cloudflare.com/)

**ApexCare** is an enterprise-grade, multi-platform Healthcare Information System (HIS) and Electronic Health Records (EHR) platform engineered in **Java 17**. It synchronizes a centralized **Spring Boot 3 REST API** with three unified clients operating in real time:

1. **🌐 Web Portal (`webapp/`)**: Responsive clinical interface with custom **Maritime Navy & Medical Jade** clinical design tokens, mobile drawer navigation, sticky mobile bottom navigation, and an integrated APK download gateway.
2. **🖥️ Desktop Workstation (`:desktop`)**: Native JavaFX Windows clinical workstation (`ClinicDesk`) featuring instant 1-click credential chips, real-time inventory and expiry alerts, and dashboard snapshots.
3. **📱 Android Mobile Application (`:android`)**: Native Android point-of-care mobile client for clinicians on hospital rounds, supporting offline-resilient operations and direct APK OTA updates.

All three client applications connect to the **same centralized database** (embedded H2 with PostgreSQL compatibility mode for rapid local development, or dedicated PostgreSQL 16 for production and Docker deployments). Any patient registered, appointment scheduled, or invoice settled on one client updates all platforms immediately.

---

## 🏛️ System Architecture

```
                          +--------------------------------------------------------+
                          |                   ApexCare Clients                     |
                          +--------------------------------------------------------+
                                 |                            |               |
             +-------------------+             +--------------+               +---------------+
             |                                 |                                              |
      [ Web Portal ]                 [ Desktop Workstation ]                       [ Android App ]
      HTML5 / CSS3 / Vanilla JS      JavaFX 17 (`ClinicDesk`)                      Native Android SDK
      (Responsive / Mobile Drawer)   (Local Clinical Stations)                     (Point-of-Care Rounds)
             |                                 |                                              |
             +-------------------+             +--------------+               +---------------+
                                 |                            |               |
                                 \____________________________/_______________/
                                              |
                                              | HTTPS / REST / JSON (Basic Auth)
                                              v
                              +-------------------------------+
                              |    Cloudflare Global Edge     |  <-- Optional 1-Click Public Tunnel
                              |     (start-online.bat)        |      (zero router config / zero credit card)
                              +-------------------------------+
                                              |
                                              v
                              +-------------------------------+
                              |  Spring Boot 3 Central Server |
                              |      (Port: 8088 / 8080)      |
                              +-------------------------------+
                              | - WorkflowController (REST)   |
                              | - WebPortalController (UI)    |
                              | - DownloadController (APK)    |
                              | - SecurityConfiguration (Auth)|
                              | - Flyway DB Migrations (V1-V6)|
                              +-------------------------------+
                                              |
                                              | JDBC / HikariCP
                                              v
                              +-------------------------------+
                              |     Relational Database       |
                              |  - Local Dev: Embedded H2     |
                              |  - Production: PostgreSQL 16  |
                              +-------------------------------+
```

---

## 🌟 Clinical Subsystems & Functional Modules

| Subsystem | Key Capabilities & Clinical Workflows |
| :--- | :--- |
| **🧑‍⚕️ Master Patient Index (MPI)** | Centralized outpatient intake, unique Medical Record Numbers (`MRN`), demographic history auditing, kin emergency contact mapping, and patient duplicate review & merge workflows. |
| **👨‍⚕️ Clinician Roster & Scheduling** | Departmental staff directories, clinical credentials tracking, outpatient consultation fee schedules, and shift availability management. |
| **📅 Outpatient Scheduling (OPD)** | Multi-specialty booking engine with **transactional double-booking prevention**, check-in status lifecycles (`SCHEDULED` -> `IN_CONSULTATION` -> `COMPLETED`), and waitlist management. |
| **💳 Revenue Cycle Management (RCM)** | Point-of-care invoicing engine enforcing financial contracts, multi-tier discount application, statutory GST tax computation, balance reconciliation, and printable tax receipts. |
| **📋 Electronic Health Records (EHR)** | Clinical progress notes, ICD-10 indication tracking, structured prescription dosing regimens (Rx), diagnostic lab/radiology requisitions, and longitudinal chart reviews. |
| **📦 Android APK Distribution Center** | Built-in streaming binary endpoint (`/downloads/apexcare-mobile.apk`) delivering the signed mobile application directly to Android devices without third-party app stores. |

---

## 🚀 Quickstart & Execution Guide

### Method 1: Instant Local Launch (Recommended for Windows)

ApexCare comes with pre-configured 1-click execution scripts:

1. **Start the Central Server & Web Portal**:
   Double-click `run.bat` or run in PowerShell:
   ```powershell
   .\run.bat
   ```
   *The server starts on `http://localhost:8088/`, applies Flyway database migrations (`V1` to `V6`), and immediately serves both the REST API and the Web Portal.*

2. **Launch the Desktop Workstation (`:desktop`)**:
   In a second terminal window or by double-clicking:
   ```powershell
   .\run-desktop.bat
   ```

3. **Access the Web Portal**:
   Open your browser to:
   ```
   http://localhost:8088/
   ```

---

### Method 2: 1-Click Live Public Deployment (Global Edge Tunnel)

Want to test on physical mobile phones or share with colleagues across the world without port forwarding, VPS hosting, or credit card requirements?

Double-click `start-online.bat` or run:
```powershell
.\start-online.bat
```
*This starts the local clinical server and connects a secure Cloudflare Global Edge Tunnel (`trycloudflare.com`). It will print an HTTPS URL (e.g., `https://random-name.trycloudflare.com`). Open this link on any mobile phone, tablet, or external laptop to access the live portal instantly!*

---

### Method 3: Enterprise Docker Compose (Production Environment)

To deploy the entire production stack (Spring Boot API + PostgreSQL 16 container):

```bash
# 1. Copy environment template
cp .env.example .env

# 2. Build and launch containers
docker compose up --build -d

# 3. Check health status
docker compose ps
```
The Docker stack runs the server behind a non-root system user (`hms`), with automated PostgreSQL health probes and container restart policies.

---

## 🔑 Default Demonstration Accounts

Pre-seeded staff and patient accounts for testing all clinical roles:

| Role | Username | Password | Access Scope |
| :--- | :--- | :--- | :--- |
| **System Administrator** | `admin` | `DemoAdminPass123!` | Full administrative access, staff directory, audit trails, system configuration |
| **Senior Physician** | `demo.doctor` | `DemoStaffPass123!` | OPD consultations, clinical notes, diagnosis (ICD-10), medication prescriptions |
| **Outpatient Receptionist** | `demo.reception` | `DemoStaffPass123!` | Patient registration, appointment scheduling, queue management, check-in |
| **Clinical Pharmacist** | `demo.pharmacy` | `DemoStaffPass123!` | Prescription dispensing, medicine inventory, substitution verification |
| **Patient Portal** | `PAT-1001` | `patient123` | Personal health records, appointment history, upcoming visits, billing receipts |

> **Tip**: The login screens across the Web Portal, JavaFX Desktop App, and Android Mobile App all feature **1-click instant demo chips** for rapid role testing.

---

## 📱 Mobile & Desktop Apps

### Android Mobile Client (`:android`)
- Located in `android/`.
- Pre-compiled debug APK is bundled directly inside `webapp/downloads/apexcare-mobile.apk`.
- Users can install it straight from the Web Portal login page by clicking **"Download Android App"**.
- To build the APK from source using Gradle:
  ```powershell
  cd android
  .\gradlew.bat assembleDebug
  ```

### Windows Desktop Workstation (`:desktop`)
- High-performance native JavaFX client with hardware-accelerated rendering.
- To package a standalone Windows application bundle (complete with bundled JRE):
  ```powershell
  .\gradlew.bat :desktop:packageWindowsApp
  ```

---

## 🌐 REST API Endpoints Overview

The central Spring Boot service exposes a comprehensive JSON API:

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Service health status and database connectivity | Public |
| `GET` | `/api/me` | Current authenticated staff/user profile & roles | Basic Auth |
| `GET` | `/api/patients` | Query Master Patient Index (supports search filter) | Staff |
| `POST` | `/api/patients` | Register new outpatient record | Staff |
| `GET` | `/api/doctors` | List active medical staff and OPD consultation fees | Staff |
| `GET` | `/api/appointments`| Retrieve scheduled clinic appointments | Staff |
| `POST` | `/api/appointments`| Book appointment with conflict prevention | Staff |
| `POST` | `/api/billing/invoices` | Generate GST tax invoice & calculate balance | Staff |
| `GET` | `/api/records` | Query Electronic Health Records and notes | Staff / Doctor |
| `GET` | `/downloads/apexcare-mobile.apk` | Stream latest Android APK binary | Public |

---

## 📂 Project Directory Structure

```
Hospital_Management_System/
├── android/                             # Native Android Mobile Client
│   ├── app/src/main/java/               # MainActivity.java, UI & HTTP Client
│   └── build.gradle                     # Android SDK build configuration
├── desktop/                             # JavaFX Windows Desktop Application
│   ├── src/main/java/                   # HospitalDesktopApp.java, ApiClient.java
│   ├── src/main/resources/clinic.css    # Desktop Workstation clinical theme
│   └── build.gradle                     # OpenJFX & jpackage bundling
├── server/                              # Central Spring Boot 3 REST API
│   ├── src/main/java/com/hospital/management/server/
│   │   ├── api/                         # WorkflowController, DownloadController
│   │   ├── config/                      # SecurityConfiguration, DemoDataSeeder
│   │   └── service/                     # Duplicate review, clinical workflows
│   ├── src/main/resources/
│   │   ├── application.yml              # Server port (8088), H2 & Flyway config
│   │   └── db/migration/                # Flyway SQL migrations (V1 to V6)
│   └── build.gradle                     # Spring Boot & Webapp asset bundler
├── webapp/                              # Modern Responsive Web Portal
│   ├── css/style.css                    # Maritime Navy & Medical Jade design tokens
│   ├── js/app.js                        # Navigation, mobile drawer, API bridge
│   ├── downloads/                       # Direct APK binary repository
│   ├── index.html                       # Clinical Operations Dashboard
│   ├── login.html                       # Staff & Patient Gateway
│   ├── patients.html                    # Master Patient Index
│   ├── appointments.html                # Outpatient Scheduling
│   ├── billing.html                     # RCM & Invoicing
│   └── patient-portal.html              # Patient Self-Service
├── .tools/                              # Bundled Cloudflare Tunnel client
├── docs/                                # Technical specifications
│   ├── CODE_EXPLANATION.md              # Historical line-by-line guide
│   └── OPERATIONS.md                    # Deployment & maintenance guide
├── compose.yaml                         # Production Docker Compose definition
├── Dockerfile                           # Multi-stage production container build
├── DEVELOPER_STUDY_GUIDE.md             # In-depth architectural & student study guide
├── run.bat                              # 1-Click Server launcher
├── run-desktop.bat                      # 1-Click Desktop launcher
└── start-online.bat                     # 1-Click Cloudflare live deployment launcher
```

---

## 📚 Complete Developer Study & Architecture Guide

For an in-depth, concept-by-concept educational guide covering:
- How this system was built step-by-step
- Why specific tools, libraries, and design patterns were selected
- Deep code walkthroughs for Java, Spring Boot, JavaFX, and Android
- What Docker is and how containerization works
- Interview & Viva questions with model answers

👉 **Read the comprehensive [DEVELOPER_STUDY_GUIDE.md](file:///C:/Users/ikp14/Hospital_Management_System/DEVELOPER_STUDY_GUIDE.md)**.

---

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.