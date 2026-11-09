# Hospital Management System

A guided Java learning project for an outpatient hospital management system, designed with real-world workflows in mind.

## Current scope

- India-first, single-facility outpatient workflow
- Staff users on Android and Windows
- Patient registration, doctor management, appointments, consultation records, prescriptions, medicine inventory, and billing
- Java for application code; supporting formats such as SQL, XML, and Gradle configuration are also part of the project

See [the project brief](docs/PROJECT_BRIEF.md) for the agreed scope, workflow, architecture direction, and deferred features.

## Project status

Assignment implementation in progress. The Java API and Windows desktop client compile, and the Android Java client compiles. A clean H2 database has applied all six migrations, and the synthetic staff-to-payment API workflow has been exercised. This is not a verified clinical product and does not claim production readiness or legal compliance. See the [assignment demo sequence](docs/ASSIGNMENT_DEMO.md), [operations and current limits](docs/OPERATIONS.md), and the [project brief](docs/PROJECT_BRIEF.md).

## Start locally

Use JDK 17. For a fresh development database, set an initial administrator password, then start the API and Windows app in separate PowerShell windows:

```powershell
$env:HMS_ADMIN_PASSWORD = 'choose-a-unique-password-of-at-least-12-characters'
.\gradlew.bat :server:bootRun
```

```powershell
.\gradlew.bat :desktop:run
```

To create a portable Windows app folder with its own Java runtime:

```powershell
.\gradlew.bat :desktop:packageWindowsApp
```

The app image is created under `desktop/build/package/ClinicDesk/`.

The Android project is in `android/`. Setup, database, Compose deployment, HTTPS, and backup guidance is in [docs/OPERATIONS.md](docs/OPERATIONS.md).

## Learning guide

The requested detailed line-by-line explanations will be added as a consolidated guide after the implementation work is complete.
