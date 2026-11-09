# Fictional India-style demonstration data

The application includes an **opt-in demo profile** for classroom demonstrations. It creates fictional patients, one doctor, appointments, clinic hours, medicine catalogue entries, and opening stock. It does not create diagnoses, prescriptions, or clinical advice.

Do not enable this profile against a real clinic database. Demo patients use intentionally invalid phone numbers beginning with `+91 00000`; medicine prices, supplier, stock quantities, batches, and expiry dates are illustrative values created for this project.

## Enable demo records

In PowerShell, from the repository folder, set separate passwords for the first admin and the demo staff accounts, then start the service with the `demo` profile:

```powershell
$env:HMS_ADMIN_PASSWORD = 'ChooseAnAdminPassword123!'
$env:HMS_DEMO_STAFF_PASSWORD = 'ChooseADemoStaffPassword123!'
.\gradlew.bat :server:bootRun --args="--spring.profiles.active=dev,demo --server.port=8081"
```

The demo seeder runs once per database. If `DEMO-IN-001` already exists, startup leaves all demo records as they are. Demo staff usernames are `demo.reception`, `demo.doctor`, `demo.pharmacy`, and `demo.billing`; each uses the password set in `HMS_DEMO_STAFF_PASSWORD`. The initial administrator is still `admin` and uses `HMS_ADMIN_PASSWORD` only when the database has no staff accounts.

If the app was already started on port 8081, stop it before starting with this profile. The Windows sign-in address is `http://localhost:8081`; for the Android emulator, enter `http://10.0.2.2:8081`.

## Medicine catalogue reference

The generic names and listed example forms used in the demo catalogue are based on entries in India's National List of Essential Medicines (NLEM) 2022, published by the Central Drugs Standard Control Organisation. The demo does not import the full list and does not treat inclusion in NLEM as a prescribing recommendation. See the [official NLEM 2022 document](https://cdsco.gov.in/opencms/resources/UploadCDSCOWeb/2018/UploadPublic_NoticesFiles/reportnlem2022.pdf).
