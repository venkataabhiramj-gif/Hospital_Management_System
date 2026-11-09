# Local setup and operations

## Scope today

The repository contains an outpatient staff application foundation: a Spring Boot Java service, a JavaFX Windows client, a native Java Android client, SQL migrations, role-based access, audit events, and a Docker Compose deployment template. Both clients provide role-aware sign-in and the patient, appointment, consultation, pharmacy, billing, and password workflows. The Windows client also provides staff and doctor administration, schedules, audit review, and daily reporting. The Android administrator workspace includes staff and doctor setup, schedules, audit review, and daily reporting.

This is an early implementation, not a verified clinical product or a claim of production readiness or legal compliance. The API and Windows client compile; the Android Java client compiles. A clean H2 database has applied all six migrations, and a synthetic API workflow has exercised staff/doctor setup, patient registration, booking/check-in, consultation, prescription substitution, stock receipt/dispensing, invoicing, payment, and receipt history. The clients have not been exercised interactively as a complete journey, and the Compose deployment has not been built or started. A real clinic must first review its workflow, train staff, configure a secure network, exercise backup/restore, review privacy and applicable regulations, and run a controlled pilot with synthetic or otherwise properly authorized data.

## Run on Windows for development

Requirements: JDK 17. The Gradle wrapper downloads the pinned Gradle distribution and dependencies on first use.

1. Open PowerShell in the repository folder.
2. Set an initial admin password before the first database is created:

   ```powershell
   $env:HMS_ADMIN_PASSWORD = 'choose-a-unique-password-of-at-least-12-characters'
   ```

3. Start the service:

   ```powershell
   .\gradlew.bat :server:bootRun
   ```

   The development database is stored under `data/`. Keep this directory private and back it up if it contains anything you need. If no password was set on a fresh development database, the service generates one and prints it once in its startup log.

4. In a second PowerShell window, start the Windows client:

   ```powershell
   .\gradlew.bat :desktop:run
   ```

   For a portable Windows app folder that includes a Java runtime, run `.\gradlew.bat :desktop:packageWindowsApp`. The output is in `desktop/build/package/ClinicDesk/`; this is an app folder, not a signed installer.

5. Sign in to `http://localhost:8080` with the admin username and password. Create staff accounts, then a doctor profile and weekly clinic hours before booking appointments. The default doctor visit length is 20 minutes.

The Android app can be opened as a separate Gradle project from the `android` folder in Android Studio. Install Android SDK 36 there, or build from a terminal in the `android` folder:

```powershell
.\gradlew.bat :app:assembleDebug
```

The debug APK is written to `app\build\outputs\apk\debug\app-debug.apk`; use it for development only. In the Android emulator, the default service address `http://10.0.2.2:8080` reaches the development computer. Debug HTTP is allowed only for that emulator bridge; Windows and physical Android devices must use HTTPS for clinic connections.

The release variant rejects plain HTTP. To sign a release APK, create and securely back up a release keystore, then set `HMS_ANDROID_KEYSTORE_PATH`, `HMS_ANDROID_KEYSTORE_PASSWORD`, `HMS_ANDROID_KEY_ALIAS`, and `HMS_ANDROID_KEY_PASSWORD` in the build environment before running `.\gradlew.bat :app:assembleRelease`. Without those values, Gradle produces an unsigned APK under `app\build\outputs\apk\release\`; never commit the keystore or its passwords.

## Run the PostgreSQL deployment template

Docker Compose binds the API only to loopback. Place a reviewed HTTPS reverse proxy in front of it before connecting Windows or Android devices. Basic authentication must travel over HTTPS. Do not expose port 8080 directly to the internet or an untrusted network.

1. Copy `.env.example` to `.env` and set different, unique database and administrator passwords. Keep `.env` private; it is ignored by Git.
2. Start the services with `docker compose up --build -d`.
3. Confirm startup with `docker compose logs api` and check `http://localhost:8080/api/health` from the host.
4. Configure a TLS reverse proxy and point both clients to its HTTPS address.

The first administrator is created only when the database has no staff accounts. Changing `HMS_ADMIN_PASSWORD` later does not change an existing account password. Staff can change their own password in either client; an administrator can reset another staff password through the API. Share reset credentials through a separate secure channel.

## Database backups

The Compose database uses a named volume. A volume alone is not a backup. Store encrypted backups outside the machine that runs the application and regularly exercise restoration into a separate environment.

Create a SQL backup:

```powershell
docker compose exec -T db pg_dump -U hms hospital > .\hospital-backup.sql
```

Restore only into a deliberately selected, stopped or isolated target database after protecting any existing data:

```powershell
Get-Content .\hospital-backup.sql | docker compose exec -T db psql -U hms -d hospital
```

The commands above are operational examples, not a complete backup policy. Define retention, encryption, access controls, off-site storage, recovery objectives, restore approval, and periodic restore drills with the facility's administrator.

## Workflow notes and current limits

- Appointments require configured doctor hours. The service derives each appointment's end time from the selected doctor's configured visit length, and rejects conflicts while the doctor row is locked.
- Staff can cancel an appointment with a reason. A no-show requires a reason and is accepted only after the scheduled end time. Appointment status history records the staff member, time, previous state, new state, and any reason; existing appointments receive a baseline history entry when the migration is applied.
- A consultation can begin only after check-in. Finalized encounters cannot be overwritten; corrections are appended as addenda.
- A doctor can review a patient's prior encounters while that doctor has a booked, checked-in, or in-progress appointment for the patient. Only the doctor who authored an encounter can change it or append its correction.
- Patient demographic corrections require a reason and preserve the before-and-after values. Administrators can review the correction history; staff should search for the patient first and enter the complete corrected details.
- Registration and demographic correction check exact patient names and normalized phone numbers against other active records. Staff must review listed candidates before confirming separate records and provide a reason; administrators can review these decisions later. No automatic merge is performed.
- Administrators can merge confirmed duplicate patient records with a required reason. The source record remains inactive and traceable; appointments, encounters, and invoices move to the surviving record. Merges are rejected when the records have overlapping active appointments with the same doctor.
- Stock is received into identified medicine batches. Dispensing uses eligible, unexpired batches in earliest-expiry order. The transaction rolls back if the full requested quantity is unavailable.
- Pharmacy staff can review the latest stock movements with the batch, quantity change, reason, staff member, and timestamp.
- A doctor or administrator may approve one substitution for a prescription item after the visit is finalized and before any dispensing or invoicing. Record the clinical reason; pharmacy staff can review the substitution history before dispensing.
- A pharmacy may dispense a partial quantity by entering the quantity actually supplied. Only medicines dispensed before invoice creation are billed; the invoice locks further dispensing for that visit. Review unfilled prescription quantities before creating the invoice.
- Supplier returns reduce the selected batch's stock and create a negative movement. Medicine returned by a patient must never be added back to sellable stock; patient-return disposition workflows are not implemented.
- Invoices snapshot service and medicine prices. Payments cannot exceed the outstanding amount. Refunds are capped at the original payment; an invoice with net payments must be refunded before it can be voided.
- Billing staff can open an invoice's itemized charges and payment/receipt history from either client.
- Patient-return disposition, inter-facility stock transfers, controlled-medicine workflows, tax/GST configuration, invoice printing, receipt printing, detailed audit filters, and full offline operation are not implemented.
- A portable Windows app image is generated under `desktop/build/package/ClinicDesk/`; it has not been signed, and no installer is generated. Debug and unsigned release Android APKs are built; a release signing key is not configured. The Java service and Windows client have been compiled and packaged, and the synthetic database-backed API workflow has been exercised end to end. The Windows and Android screens have not been verified through a complete interactive journey. Docker is not installed on the development computer, so the Compose deployment template has not been built or started here.
- Detailed audit filters and richer operational reports remain unfinished.
- The application does not diagnose, recommend treatment, validate prescriptions clinically, determine tax treatment, or certify regulatory compliance.
