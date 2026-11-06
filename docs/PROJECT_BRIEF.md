# Project Brief: Hospital Management System

## Status

Planning brief, based on the current project discussion. The repository contains no application source code yet. Requirements and architecture remain open to refinement before implementation.

## Product direction

Build a Java learning project for a real-world-oriented hospital management system. The first target is one outpatient facility in India, with staff using Android and Windows applications. The project should be taught and built incrementally so contributors understand the Java concepts, tools, and code introduced at each milestone.

“Production-oriented” describes the design goal. It is not a claim that the unfinished project is safe or compliant for real patient care. A real deployment would require implementation and verification, review by qualified healthcare and security professionals, and a controlled pilot with the intended facility.

## First-release users

- **Administrator:** manages staff accounts, roles, and facility configuration.
- **Receptionist:** finds or registers patients, manages appointments, and checks patients in.
- **Doctor:** reviews relevant history and records the consultation and any prescription.
- **Pharmacist:** maintains medicine stock and records dispensing against a prescription.
- **Billing staff:** prepares charges, records payments, and issues receipts.

The first Android experience is for staff. Patient-facing booking or record access is deferred.

## Outpatient visit workflow

1. Reception searches for an existing patient before creating a registration, books an appointment, and checks the patient in.
2. The doctor opens the visit, reviews relevant history, and records the consultation and any prescription.
3. Pharmacy staff review the prescription, dispense medicines, and record the stock movement against the relevant medicine batch.
4. Billing staff prepare visit charges, record payment, and issue a receipt.
5. Authorized administrators manage access and review operational activity and reports.

Clinical entries are recorded by authorized staff. The application is not intended to independently diagnose patients or recommend treatment.

## Workflow rules captured so far

These are agreed working defaults for requirements discovery. They still need review with the intended facility's staff before any real deployment.

### Patient registration

- Search for existing patients using more than one available detail, such as facility ID, name, date of birth, or contact number.
- If there is no likely match, create a patient record with a facility-generated patient number.
- Collect only the information the facility needs to identify and contact the patient. Aadhaar or ABHA is not a required identifier for the first release.
- Flag possible duplicates for authorized staff review; do not merge records automatically.
- Make demographic corrections traceable.

### Appointments and check-in

- Staff can book, reschedule, or cancel an appointment and check a patient in.
- Track appointment states: booked, checked in, in progress, completed, cancelled, and no-show.
- Prevent overlapping appointments for the same doctor.
- Support walk-ins as a distinct visit type.

### Consultation records

- Link each consultation to a patient, visit, author, and date/time.
- Let the doctor review relevant history, allergies, and current medicines, then record the consultation and any prescription.
- Combine structured information, such as vitals, with clinician-authored notes.
- Preserve traceability when a finalized record needs correction; do not silently replace the earlier entry.
- The first version records decisions by authorized clinicians and does not independently diagnose or recommend treatment.

### Medicine dispensing

- Keep the medicine catalogue separate from stock batches. A batch includes its batch number, expiry, quantity, supplier, and purchase details.
- Record each stock change as a movement: received, dispensed, returned, or adjusted. Adjustments require a reason and an accountable staff member.
- A prescription does not reduce stock. Stock changes when pharmacy staff record the actual dispensing.
- Link dispensed quantities to the prescription, patient visit, and batch.
- Prevent dispensing from expired or empty batches. Where more than one valid batch is available, suggest the batch that expires soonest.
- A medicine substitution requires approval from an authorized clinician. Rules for partial dispensing still need to be defined.

### Billing

- Build the bill from the facility's configured charges and items actually dispensed for the visit.
- Preserve the price used on an invoice if the facility later changes its price list.
- Track payment status, including partial payment, and issue a unique receipt for each payment.
- Require authorization and an audit trail for corrections, refunds, and voided bills.

## Functional scope for refinement

- **Patient registration:** patient search, unique facility identifier, minimal demographic/contact details, duplicate checks, and traceable demographic corrections.
- **Doctor management:** staff profiles, specialties, availability, and appointment assignment.
- **Appointments:** booking, rescheduling, cancellation, check-in, walk-ins, lifecycle status, and prevention of doctor schedule overlaps.
- **Consultation records:** dated and attributed encounter notes, relevant history, allergies, current medicines, structured observations, and prescriptions, with traceable corrections.
- **Medicine inventory:** medicine catalogue, batch/lot and expiry tracking, stock received, dispensed, returned or adjusted, movement history, and low-stock/expiry alerts.
- **Billing:** configured charges, invoices, payment status including partial payment, receipts, and traceable corrections, refunds, or voids.
- **Administration:** staff accounts, role-based permissions, audit history, and operational reports.
- **Reliability and privacy foundations:** backups and restore procedures, clear error handling, data minimization, secure access, and an audit trail for sensitive record and stock changes.

Detailed fields, business rules, and report formats need workflow review before they are treated as final requirements.

## Open workflow decisions

- Required and optional registration fields, and workflows for children, emergency cases, and patients whose identity is not yet known.
- Consultation template, vital-sign fields, and how the facility reviews and corrects finalized records.
- Rules for partial dispensing, medicine returns, and any regulated-medicine workflows.
- Facility billing policy, including tax configuration, invoice corrections, and refund approvals.
- Connectivity assumptions and what staff can safely do during a service outage.

## Platform and architecture direction

The current proposal is:

- A Java backend service as the shared source of business rules and data access.
- A native Android client written in Java.
- A Windows desktop client written in Java using JavaFX.
- A central relational database, with PostgreSQL as the leading candidate.
- Client applications communicate with the service; they do not connect directly to the database.

This is a proposed direction, not an implemented or final architecture. We will validate the exact Java version, frameworks, database, build tooling, deployment model, and connectivity assumptions before creating the application skeleton. Android supports a subset of standard JDK APIs, so shared code and dependencies must be checked for Android compatibility.

Java-only means Java for application code. Android resources, SQL, build configuration, and other platform metadata use their respective supporting formats and will be explained as they are introduced.

## Quality and deployment requirements to investigate

- Access should follow staff responsibilities, and sensitive actions should be attributable to a user.
- Patient information should be collected and retained only for defined purposes, with clear notices and suitable consent workflows where required.
- The system needs a tested backup and restore process, secure communication, secure credential handling, and an incident response plan before any real deployment.
- Appointment, billing, and inventory changes must remain consistent when several authorized users work at the same time.
- India-specific requirements and applicable state/facility rules must be reviewed with qualified experts. This brief is not legal advice and does not establish compliance.
- Future expansion across Asia will require country-specific review, localization, currencies, time zones, and configurable workflows; India-specific assumptions must not be treated as universal.

## Deferred from the first release

- Inpatient admissions, beds, wards, nursing, and discharge workflows.
- Patient-facing mobile features.
- iOS and macOS applications.
- Multi-facility administration and cross-country deployments.
- ABDM integration. Interoperability should remain an architectural consideration, but integration is not a first-release commitment.

## Official references for discovery

These sources inform further requirements work. They do not by themselves determine whether a specific deployment is compliant.

- [Digital Personal Data Protection Rules, 2025 — Gazette of India](https://www.meity.gov.in/static/uploads/2025/11/53450e6e5dc0bfa85ebd78686cadad39.pdf)
- [Electronic Health Record Standards for India, 2016 — Ministry of Health and Family Welfare](https://www.mohfw.gov.in/sites/default/files/EMR-EHR_Standards_for_India_as_notified_by_MOHFW_2016_0.pdf)
- [Health Data Management Policy — Ayushman Bharat Digital Mission](https://abdm.gov.in/publications/policies_regulations/health_data_management_policy)
- [Drugs Rules — Central Drugs Standard Control Organisation](https://www.cdsco.gov.in/opencms/opencms/en/Acts-and-rules/Drugs-Rules/)
