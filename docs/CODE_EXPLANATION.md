# 🏥 Hospital Management System — Comprehensive Code & Viva Guide
**Course:** Object-Oriented Programming through Java / Web Technologies  
**Target Audience:** CSE 2nd Year, 1st Semester (SRM University AP)  
**Authors:** Contributors to `Hospital_Management_System`

---

## 📑 Table of Contents
1. [Architectural Overview & Academic Concepts](#1-architectural-overview--academic-concepts)
2. [Database Layer (JDBC & Connection Management)](#2-database-layer-jdbc--connection-management)
3. [Custom Exception Hierarchy](#3-custom-exception-hierarchy)
4. [OOP Domain Models (Inheritance, Polymorphism & Encapsulation)](#4-oop-domain-models)
5. [Interface Contracts & DAO Layer](#5-interface-contracts--dao-layer)
6. [Web Server & REST Controller](#6-web-server--rest-controller)
7. [Frontend Architecture (HTML5, CSS3 & JavaScript)](#7-frontend-architecture)
8. [Common Lab Viva Questions & Model Answers](#8-common-lab-viva-questions--model-answers)

---

## 1. Architectural Overview & Academic Concepts

The system follows a strict **Layered MVC (Model-View-Controller) / DAO (Data Access Object) Pattern**:
- **Presentation Layer (`webapp/`)**: Responsive HTML5 web portal with CSS3 design tokens and asynchronous JavaScript (`fetch` API).
- **Controller Layer (`com.hospital.web`)**: Java `HttpServer` routing requests to REST endpoints and serving static web assets.
- **Service & DAO Contracts (`com.hospital.interfaces`)**: Interfaces defining strict contracts for data persistence and financial calculations.
- **Data Access Layer (`com.hospital.dao`)**: Parameterized JDBC SQL operations (`PreparedStatement`) with zero SQL injection risk.
- **Domain Models (`com.hospital.models`)**: Encapsulated entities using abstract classes and inheritance (`Person` -> `Doctor`, `Patient`).
- **Database Layer (`com.hospital.config`)**: Thread-safe Singleton database manager with embedded SQLite zero-setup configuration.

---

## 2. Database Layer (`com.hospital.config`)

### File: `DatabaseConfig.java`

```java
public class DatabaseConfig {
    private static DatabaseConfig instance;
    private static final String DB_FILE = "hospital.db";
    private static final String SQLITE_URL = "jdbc:sqlite:" + DB_FILE;
```
- **What it does:** Manages database connectivity and schema initialization using the **Singleton Pattern**.
- **How it works:**
  1. `private static DatabaseConfig instance;` ensures only one instance exists across the entire JVM lifecycle.
  2. The constructor is declared `private`, preventing other classes from creating new connection managers via `new DatabaseConfig()`.
  3. `public static synchronized DatabaseConfig getInstance()` provides global, thread-safe access to the single instance.
  4. In the constructor, `Class.forName("org.sqlite.JDBC");` loads the SQLite JDBC driver bytecode dynamically into JVM memory.
  5. `initializeDatabase()` uses `CREATE TABLE IF NOT EXISTS` statements to build 5 relational tables (`patients`, `doctors`, `appointments`, `bills`, `medical_records`).
  6. `seedInitialData()` automatically checks if tables are empty and injects realistic demonstration data so the system works out-of-the-box.
- **Why it was written this way:**
  - *Academic Viva Question:* "Why use the Singleton pattern for the database?"
  - *Answer:* Opening multiple database connection managers wastes memory and system socket handles. Singleton centralizes connection settings, ensuring all DAOs share consistent configuration and automated table initialization.

---

## 3. Custom Exception Hierarchy (`com.hospital.exceptions`)

Instead of throwing generic `Exception` or `RuntimeException`, the system defines a clean hierarchy inheriting from `HospitalException`:

```
java.lang.Exception (Checked)
       └── HospitalException (Base Domain Exception)
             ├── PatientNotFoundException
             ├── DoctorNotFoundException
             ├── AppointmentConflictException
             ├── InvalidBillingException
             └── DatabaseException
```

### Detailed Breakdown:
1. **`HospitalException.java`**:
   - *What:* Base checked exception extending `java.lang.Exception`.
   - *Why:* Forces the compiler to check that callers handle hospital-specific domain failures using `try-catch` blocks.
2. **`AppointmentConflictException.java`**:
   - *Fields:* `doctorId`, `date`, `timeSlot`.
   - *What & How:* Thrown when an appointment booking collides with an existing active booking for the same doctor at the same date and slot.
   - *Why:* Prevents double-booking at the business-logic layer and maps cleanly to HTTP `409 Conflict`.
3. **`InvalidBillingException.java`**:
   - *What & How:* Thrown by `Bill.calculateFinalTotal()` if consultation fee is negative, discount exceeds 100%, or tax is negative.
   - *Why:* Enforces domain integrity—financial calculations must never process invalid or negative numbers.
4. **`PatientNotFoundException.java` & `DoctorNotFoundException.java`**:
   - *What:* Thrown when queries for a specific ID return no database records.
   - *Why:* Maps directly to HTTP `404 Not Found`.
5. **`DatabaseException.java`**:
   - *What:* Wraps lower-level `java.sql.SQLException`.
   - *Why:* Decouples upper application layers from SQL-specific exceptions. If the underlying database changes from SQLite to MySQL or PostgreSQL, upper layers still catch `DatabaseException`.

---

## 4. OOP Domain Models (`com.hospital.models`)

### A. Abstract Class: `Person.java`
- **Core OOP Concepts:** **Abstraction** and **Encapsulation**.
- **Key Code:**
  ```java
  public abstract class Person {
      protected String id;
      protected String name;
      protected String phone;
      protected int age;
      ...
      public abstract String getRole();
  ```
- **What & How:**
  - Declared `abstract`, meaning you cannot do `new Person()`.
  - Defines shared attributes for all human actors in the hospital (`id`, `name`, `email`, `phone`, `age`, `gender`).
  - Has an abstract method `public abstract String getRole();` that subclasses *must* implement.
  - Implements **Encapsulation** in `setAge(int age)`: checks `if (age < 0 || age > 130)` and throws `IllegalArgumentException`.
- **Why:** Prevents duplicate code across `Patient` and `Doctor`, enforcing standard person attributes across the system.

### B. Subclasses: `Patient.java` and `Doctor.java`
- **Core OOP Concepts:** **Inheritance** and **Polymorphism**.
- **Key Code:**
  ```java
  public class Patient extends Person {
      private String bloodGroup;
      private String address;
      ...
      @Override
      public String getRole() { return "PATIENT"; }
  }
  ```
  ```java
  public class Doctor extends Person {
      private String specialization;
      private double consultationFee;
      ...
      @Override
      public String getRole() { return "DOCTOR"; }
  }
  ```
- **What & How:**
  - `extends Person` inherits all fields and methods from `Person`.
  - Constructors call `super(id, name, email, phone, age, gender);` to initialize the base class state.
  - Both override `getRole()` (Runtime Polymorphism).
- **Why:** Demonstrates hierarchical domain modeling. A Doctor is-a Person; a Patient is-a Person.

### C. Financial Model: `Bill.java`
- **Core OOP Concepts:** **Interface Implementation** & **Contract-Driven Programming**.
- **Key Code:**
  ```java
  public class Bill implements Billable {
      ...
      @Override
      public double calculateSubtotal() { return consultationFee + treatmentCharges + medicineCharges; }
      @Override
      public double calculateDiscountAmount() { return (calculateSubtotal() * discountPercent) / 100.0; }
      @Override
      public double calculateTaxAmount() { return ((calculateSubtotal() - calculateDiscountAmount()) * taxPercent) / 100.0; }
      @Override
      public double calculateFinalTotal() throws InvalidBillingException { ... }
  ```
- **What & How:** Implements all financial computation methods defined in `Billable`. Enforces validation before returning final total.

---

## 5. Interface Contracts & DAO Layer (`com.hospital.interfaces` & `com.hospital.dao`)

### Why use DAOs with Interfaces?
- The Interface defines **WHAT** operations can be performed (e.g., `addPatient`, `getPatientById`).
- The DAO Implementation defines **HOW** operations are executed using JDBC and SQL queries.
- If the university requires switching from SQLite to MySQL, only the connection URL and DAO implementations are touched—the controllers and services never change!

### Safe JDBC Execution with `PreparedStatement`:
Example from `PatientDAOImpl.java`:
```java
String sql = "INSERT INTO patients (id, name, email, phone, age, gender, blood_group, address, emergency_contact, registered_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
try (Connection conn = dbConfig.getConnection();
     PreparedStatement pstmt = conn.prepareStatement(sql)) {
    pstmt.setString(1, p.getId());
    pstmt.setString(2, p.getName());
    ...
    pstmt.executeUpdate();
}
```
- **What & How:**
  - `?` placeholders represent parameters.
  - Parameter values are sent separately from the SQL statement structure.
  - `try (...)` is **try-with-resources**: automatically closes `conn` and `pstmt` even if an exception occurs, avoiding connection pool leaks.
- **Why:** **Prevents SQL Injection Attacks**. If an attacker inputs `' OR '1'='1`, `PreparedStatement` treats it purely as literal string data rather than executable SQL commands.

---

## 6. Web Server & REST Controller (`com.hospital.web`)

### Embedded HTTP Architecture:
- Uses `com.sun.net.httpserver.HttpServer` from Java standard library.
- Starts a lightweight multi-threaded server on port `8088`.
- Routes:
  - `/` $\rightarrow$ `StaticFileHandler` (serves `index.html`, `patients.html`, `doctors.html`, `appointments.html`, `billing.html`, `records.html`, CSS, JS).
  - `/api/*` $\rightarrow$ `ApiHandler` (processes JSON and Form submissions for all 5 hospital modules).

---

## 7. Frontend Architecture (`webapp/`)

- **Vanilla Modern JavaScript**: No heavy frameworks (React/Angular) required—uses native `async/await` and `fetch()` to call Java backend endpoints.
- **CSS Design Tokens**: Clean hospital aesthetic with responsive grid and flexbox layouts.
- **Real-Time Client Feedback**:
  - Live billing subtotal, discount, and GST calculation preview.
  - Conflict warning toasts when an appointment slot is already booked.

---

## 8. Common Lab Viva Questions & Model Answers

### Q1: What is the difference between Method Overloading and Method Overriding in this project?
- **Overriding (Runtime Polymorphism):** In `Person`, `getRole()` is abstract. `Patient` and `Doctor` both provide their own specific implementations with `@Override`. The JVM determines which method to call at runtime based on the actual object instance.
- **Overloading (Compile-Time Polymorphism):** In `PatientDAO`, `searchPatients(String keyword)` vs `getAllPatients()`, or constructors with different parameter signatures (`Person()` vs `Person(id, name, ...)`).

### Q2: Why did you use `PreparedStatement` instead of `Statement`?
1. **Security:** `PreparedStatement` pre-compiles the SQL query and binds parameters safely, completely preventing SQL Injection.
2. **Performance:** The database engine parses and compiles the query execution plan once, reusing it for subsequent executions with different parameters.

### Q3: What is the purpose of `try-with-resources`?
Introduced in Java 7, `try-with-resources` ensures that any object implementing `java.lang.AutoCloseable` (such as `Connection`, `PreparedStatement`, `ResultSet`, `InputStream`) is automatically closed when execution exits the `try` block, preventing database connection leaks and memory leaks.

### Q4: Why use an Interface like `Billable` instead of putting everything in `Bill`?
An interface defines an abstract contract. If tomorrow the hospital introduces `PharmacySale`, `EmergencyAmbulanceCharge`, or `DiagnosticPackage`, all of them can implement `Billable` and be processed identically by billing calculators without modifying existing code (following the **Open/Closed Principle**).
