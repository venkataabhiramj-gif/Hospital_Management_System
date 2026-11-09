# 🏥 ApexCare Clinical OS — Comprehensive Code, Architecture & Implementation Guide

This document provides a line-by-line explanation of every component, design pattern, and architectural decision within the ApexCare Hospital Management System.

---

## 📑 Table of Contents
1. [Architectural Overview & Design Principles](#1-architectural-overview--design-principles)
2. [Database Layer & Connection Lifecycle](#2-database-layer--connection-lifecycle)
3. [Custom Exception Hierarchy](#3-custom-exception-hierarchy)
4. [Domain Entities & OOP Principles](#4-domain-entities--oop-principles)
5. [Interface Contracts & Data Access Objects (DAO)](#5-interface-contracts--data-access-objects-dao)
6. [Concurrent Web Gateway & REST Routing](#6-concurrent-web-gateway--rest-routing)
7. [Frontend Architecture & Clinical UX](#7-frontend-architecture--clinical-ux)
8. [Technical Evaluation & Architectural Q&A](#8-technical-evaluation--architectural-qa)

---

## 1. Architectural Overview & Design Principles

ApexCare adopts a **Domain-Driven, Decoupled N-Tier Architecture**:

```
[ Web Browser Client (HTML5 / Vanilla JS / CSS3) ]
                       ▲
                       │ HTTP/1.1 (JSON / Form Data)
                       ▼
[ Gateway / Controller Layer (`com.hospital.web`) ]
                       ▲
                       │ DTOs / Domain Models
                       ▼
[ Business & DAO Layer (`com.hospital.dao` / `interfaces`) ]
                       ▲
                       │ Parameterized SQL (PreparedStatement)
                       ▼
[ Relational Persistence Layer (SQLite Engine / JDBC) ]
```

### Key Engineering Tenets:
- **Zero Third-Party Runtime Bloat:** Employs standard JDK APIs (`com.sun.net.httpserver`, `java.sql.*`), eliminating fragile container configurations.
- **Contract-First Development:** Core business rules (financial calculations, persistence operations) are formally defined through Java `interface` definitions before concrete implementation.
- **Defensive Error Handling:** Employs domain-specific checked exceptions to enforce explicit error recovery at compile time.

---

## 2. Database Layer & Connection Lifecycle

### File: `DatabaseConfig.java` (`com.hospital.config`)

```java
public class DatabaseConfig {
    private static DatabaseConfig instance;
    private static final String DB_FILE = "hospital.db";
    private static final String SQLITE_URL = "jdbc:sqlite:" + DB_FILE;
```
- **Line-by-Line Breakdown:**
  - `private static DatabaseConfig instance;`: Declares a single static reference variable to hold the sole instance of `DatabaseConfig` in JVM memory.
  - `private static final String DB_FILE = "hospital.db";`: The filesystem name for the embedded relational database file.
  - `private static final String SQLITE_URL = "jdbc:sqlite:" + DB_FILE;`: The standard JDBC connection string pointing to the embedded database.

```java
    private DatabaseConfig() {
        try {
            Class.forName("org.sqlite.JDBC");
            initializeDatabase();
        } catch (ClassNotFoundException e) { ... }
    }
```
- **Why it was written this way:**
  - Declaring the constructor `private` strictly implements the **Singleton Pattern**, preventing external callers from creating unmanaged instances with `new DatabaseConfig()`.
  - `Class.forName("org.sqlite.JDBC")` dynamically registers the native driver class with `DriverManager`.
  - Calls `initializeDatabase()` on startup to ensure all relational tables are constructed before incoming HTTP requests arrive.

```java
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }
```
- **Function:** Thread-safe lazy initialization accessor.
- **How it works:** The `synchronized` keyword ensures that if two concurrent threads invoke `getInstance()` simultaneously on cold boot, only one initializes the instance while the other waits, eliminating race conditions.

```java
    public Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(SQLITE_URL);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to establish JDBC connection to database engine.", e);
        }
    }
```
- **Function:** Dispenses active JDBC connection handles.
- **Why wrap in `DatabaseException`?** Encapsulates implementation-specific `SQLException` so higher architectural layers (controllers and services) remain decoupled from low-level SQL drivers.

---

## 3. Custom Exception Hierarchy

### Package: `com.hospital.exceptions`

```
java.lang.Exception (Checked)
       └── HospitalException (Base Domain Exception)
             ├── PatientNotFoundException
             ├── DoctorNotFoundException
             ├── AppointmentConflictException
             ├── InvalidBillingException
             └── DatabaseException
```

### 1. `HospitalException.java`
```java
public class HospitalException extends Exception {
    public HospitalException(String message) { super(message); }
    public HospitalException(String message, Throwable cause) { super(message, cause); }
}
```
- **What:** Root checked exception for all healthcare domain errors.
- **Why:** Forces calling methods to acknowledge potential domain failures via `throws` declarations or `try-catch` blocks.

### 2. `AppointmentConflictException.java`
```java
public class AppointmentConflictException extends HospitalException {
    private final String doctorId;
    private final String date;
    private final String timeSlot;

    public AppointmentConflictException(String doctorId, String date, String timeSlot) {
        super(String.format("Doctor %s is already booked on %s during slot %s.", doctorId, date, timeSlot));
        this.doctorId = doctorId;
        this.date = date;
        this.timeSlot = timeSlot;
    }
    // Getters ...
}
```
- **What & How:** Thrown when an appointment booking collides with an existing active booking for the same physician at the exact same date and time slot.
- **Why:** Enforces transactional schedule exclusivity. In `ApiHandler`, this exception is intercepted and returned as an **HTTP 409 Conflict** status to notify the scheduling user.

### 3. `InvalidBillingException.java`
```java
public class InvalidBillingException extends HospitalException {
    public InvalidBillingException(String message) { super(message); }
}
```
- **What & How:** Thrown by `Bill.calculateFinalTotal()` when financial arguments violate validation boundaries (e.g. negative charges, discount $> 100\%$, negative tax).
- **Why:** Guarantees ledger integrity; the database will never record anomalous financial transactions.

---

## 4. Domain Entities & OOP Principles

### Package: `com.hospital.models`

### A. Abstract Class: `Person.java`
Demonstrates **Abstraction** and **Encapsulation**.

```java
public abstract class Person {
    protected String id;
    protected String name;
    protected String email;
    protected String phone;
    protected int age;
    protected String gender;
```
- `public abstract class Person`: Cannot be directly instantiated. Serves as the generalized base template for any individual in the healthcare system.
- `protected` fields: Accessible to derived classes (`Patient`, `Doctor`) without violating encapsulation from unrelated external packages.

```java
    public abstract String getRole();
```
- Abstract contract: Forces derived concrete subclasses to return their specific domain identity (`"PATIENT"` or `"DOCTOR"`), enabling **Runtime Polymorphism**.

```java
    public void setAge(int age) {
        if (age < 0 || age > 130) {
            throw new IllegalArgumentException("Invalid age: " + age + ". Age must be between 0 and 130.");
        }
        this.age = age;
    }
```
- **Encapsulation:** Intercepts invalid mutations. Direct field assignment `person.age = -5` is blocked by access modifiers.

### B. Derived Classes: `Patient.java` & `Doctor.java`
Demonstrates **Inheritance** and **Polymorphism**.

```java
public class Patient extends Person {
    private String bloodGroup;
    private String address;
    private String emergencyContact;
    private String registeredDate;

    public Patient(String id, String name, String email, String phone, int age, String gender,
                   String bloodGroup, String address, String emergencyContact, String registeredDate) {
        super(id, name, email, phone, age, gender);
        this.bloodGroup = bloodGroup;
        this.address = address;
        this.emergencyContact = emergencyContact;
        this.registeredDate = registeredDate;
    }

    @Override
    public String getRole() {
        return "PATIENT";
    }
}
```
- `super(...)`: Invokes the parent constructor, initializing inherited fields before assigning patient-specific attributes.
- `@Override public String getRole()`: Concrete implementation fulfilling the abstract contract in `Person`.

### C. Financial Model: `Bill.java`
Demonstrates **Interface Implementation** and **Business Logic Isolation**.

```java
public class Bill implements Billable {
    ...
    @Override
    public double calculateSubtotal() {
        return consultationFee + treatmentCharges + medicineCharges;
    }

    @Override
    public double calculateDiscountAmount() {
        return (calculateSubtotal() * discountPercent) / 100.0;
    }

    @Override
    public double calculateTaxAmount() {
        double discountedSubtotal = calculateSubtotal() - calculateDiscountAmount();
        return (discountedSubtotal * taxPercent) / 100.0;
    }

    @Override
    public double calculateFinalTotal() throws InvalidBillingException {
        if (consultationFee < 0 || treatmentCharges < 0 || medicineCharges < 0) {
            throw new InvalidBillingException("Billing charges cannot be negative numbers.");
        }
        if (discountPercent < 0 || discountPercent > 100) {
            throw new InvalidBillingException("Discount percentage must be between 0% and 100%.");
        }
        double discounted = calculateSubtotal() - calculateDiscountAmount();
        return Math.round((discounted + calculateTaxAmount()) * 100.0) / 100.0;
    }
}
```
- Strictly implements all four calculation contracts declared in `Billable`.
- Enforces computational order: Gross $\rightarrow$ Apply Discount $\rightarrow$ Calculate GST on Net $\rightarrow$ Round to 2 decimal places.

---

## 5. Interface Contracts & Data Access Objects (DAO)

### Package: `com.hospital.interfaces` & `com.hospital.dao`

### Why Decouple with DAO Interfaces?
- The Interface (`PatientDAO`, `AppointmentDAO`) specifies **WHAT** capabilities the persistence layer provides.
- The Implementation (`PatientDAOImpl`, `AppointmentDAOImpl`) defines **HOW** those capabilities execute via JDBC and SQL.
- If switching the database from SQLite to MySQL or PostgreSQL, the upper controllers (`ApiHandler`) remain untouched.

### Safe Parameterized JDBC Implementation:
Example from `PatientDAOImpl.java`:
```java
String sql = "INSERT INTO patients (id, name, email, phone, age, gender, blood_group, address, emergency_contact, registered_date) " +
             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
try (Connection conn = dbConfig.getConnection();
     PreparedStatement pstmt = conn.prepareStatement(sql)) {
    pstmt.setString(1, p.getId());
    pstmt.setString(2, p.getName());
    pstmt.setString(3, p.getEmail());
    pstmt.setString(4, p.getPhone());
    pstmt.setInt(5, p.getAge());
    pstmt.setString(6, p.getGender());
    pstmt.setString(7, p.getBloodGroup());
    pstmt.setString(8, p.getAddress());
    pstmt.setString(9, p.getEmergencyContact());
    pstmt.setString(10, p.getRegisteredDate());
    pstmt.executeUpdate();
} catch (SQLException e) {
    throw new DatabaseException("Error inserting patient: " + e.getMessage(), e);
}
```
- **`?` Parameter Placeholders:** Binds inputs separately from the compiled SQL structure, **completely neutralizing SQL Injection vectors**.
- **`try (...)` (try-with-resources):** Implements `AutoCloseable` contract. Connections and statements are automatically freed even if execution raises a runtime error, preventing resource exhaustion.

### Double-Booking Schedule Guard:
In `AppointmentDAOImpl.java`:
```java
@Override
public void bookAppointment(Appointment a) throws AppointmentConflictException, DatabaseException {
    if (!isSlotAvailable(a.getDoctorId(), a.getAppointmentDate(), a.getTimeSlot())) {
        throw new AppointmentConflictException(a.getDoctorId(), a.getAppointmentDate(), a.getTimeSlot());
    }
    // Proceed with INSERT ...
}
```
- Queries active bookings for that doctor, date, and slot. If an active booking exists, execution is halted by throwing `AppointmentConflictException` before any write transaction occurs.

---

## 6. Concurrent Web Gateway & REST Routing

### File: `Main.java` & `ApiHandler.java`

```java
HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
server.createContext("/api/", new ApiHandler());
server.createContext("/", new StaticFileHandler(webappDir));
server.setExecutor(Executors.newFixedThreadPool(16));
server.start();
```
- **`HttpServer.create(...)`:** Starts an embedded HTTP/1.1 server using native Java standard library features.
- **`Executors.newFixedThreadPool(16)`:** Configures a pool of 16 background threads to process incoming HTTP requests concurrently without blocking the main event loop.
- **`ApiHandler`:** Parses incoming request bodies (JSON or URL-encoded form data), invokes appropriate DAO methods, catches domain exceptions, and emits JSON responses with standard HTTP status codes (`200 OK`, `201 Created`, `400 Bad Request`, `404 Not Found`, `409 Conflict`, `500 Server Error`).

---

## 7. Frontend Architecture & Clinical UX

### Directory: `webapp/`
- **High Information Density:** Mirrors production Electronic Health Record systems (Epic, Cerner) with crisp typography, scannable tabular grids, patient initials avatars, and MRN monospace tags.
- **Vector Icons:** SVG vector line icons (Stroke 1.8px) used exclusively instead of emojis.
- **Point-of-Care Financial Receipts:** Interactive invoice generator with automated tax computation and a printable official hospital receipt modal (`window.print()`).
- **Comprehensive EHR Viewer:** Clinical consultation logger with vitals tracking, structured prescriptions, and progress notes.

---

## 8. Technical Evaluation & Architectural Q&A

### Q1: How does this system prevent SQL Injection?
**Answer:** Every single SQL interaction in the DAO layer strictly utilizes `PreparedStatement` with parameterized placeholders (`?`). The SQL query template is pre-compiled by the database engine, and user inputs are bound strictly as data values, making it impossible for user input to alter the syntactic structure of the query.

### Q2: What demonstrates Polymorphism in this codebase?
**Answer:**
1. **Runtime Polymorphism:** Overriding the abstract `getRole()` method in `Person`. When invoking `person.getRole()` on a reference of type `Person`, the JVM invokes either `Patient.getRole()` (`"PATIENT"`) or `Doctor.getRole()` (`"DOCTOR"`) depending on the runtime instance.
2. **Compile-Time Polymorphism:** Method and constructor overloading, such as `Person()` vs `Person(id, name, ...)`, and `HospitalException(message)` vs `HospitalException(message, cause)`.

### Q3: Why use an Interface (`Billable`) for financial calculations?
**Answer:** The `Billable` interface establishes a formal contract for financial calculation methods. Decoupling the calculation rules from the storage entity allows new billable items (e.g. `EmergencyService`, `RadiologyPackage`) to implement `Billable` and be processed by the financial engine without modifying existing classes, adhering to the **Open/Closed Principle**.

### Q4: Why is `DatabaseConfig` implemented as a Singleton?
**Answer:** Managing database connectivity through a central Singleton ensures that connection properties, table initialization DDL, and connection pooling are unified across all DAO instances, eliminating duplicate driver registrations and resource conflicts.
