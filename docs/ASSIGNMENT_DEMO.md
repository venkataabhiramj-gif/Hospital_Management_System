# Assignment demonstration

Use synthetic names, phone numbers, and clinical notes only. The project is an assignment implementation, not a verified clinical product.

## Start the Windows demonstration

In the repository folder, open one PowerShell window and set a temporary administrator password before starting the API:

```powershell
$env:HMS_ADMIN_PASSWORD = 'DemoAdminPass123!'
.\gradlew.bat :server:bootRun
```

In a second PowerShell window, start the Windows client:

```powershell
.\gradlew.bat :desktop:run
```

Sign in with username `admin` and the password you set. For Android, open the `android` folder in Android Studio, run the app on an emulator, and use its default service address `http://10.0.2.2:8080`.

## Demonstration sequence

1. **Administrator:** create accounts for a doctor, receptionist, pharmacist, and billing staff. Add the doctor profile and weekly clinic hours.
2. **Receptionist:** search for the synthetic patient, register them if absent, book an outpatient visit, and check them in.
3. **Doctor:** open the checked-in visit, review the patient's encounter history, record the complaint, diagnosis, observations, allergies, and current medicines, add a prescription, and finalize the encounter. If demonstrating a substitution, approve it after finalization and enter a clinical reason.
4. **Pharmacist:** add the prescribed medicine to the catalogue if needed, receive an unexpired batch, review the prescription and any substitution history, dispense the quantity supplied, then open stock movements and alerts.
5. **Billing staff:** create the invoice after dispensing, open its itemized details, and record one or more payments. The invoice view shows the receipt number and paid status.
6. **Administrator:** review recent audit events and the daily summary. For the duplicate-identity feature, use two synthetic patient records, review possible matches, then demonstrate the confirmed merge and its history.

The Windows and Android clients share the same API and database. Run the API once and keep it running while demonstrating either client. Use the IDs and patient number shown in each result when a later screen asks for a record identifier.
