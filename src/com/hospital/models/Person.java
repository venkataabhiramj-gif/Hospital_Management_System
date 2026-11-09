package com.hospital.models;

/**
 * Abstract base class representing any individual in the Hospital Management System.
 * Demonstrates Core Java OOP Principles:
 *  1. Abstraction (cannot be instantiated directly, declares abstract methods)
 *  2. Encapsulation (private/protected attributes with validated getters and setters)
 */
public abstract class Person {
    protected String id;
    protected String name;
    protected String email;
    protected String phone;
    protected int age;
    protected String gender;

    // Default Constructor
    public Person() {
    }

    // Parameterized Constructor
    public Person(String id, String name, String email, String phone, int age, String gender) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        setAge(age); // Encapsulated validation
        this.gender = gender;
    }

    // Abstract method to be implemented by subclasses (Polymorphism)
    public abstract String getRole();

    // Concrete method providing shared behavior
    public String getContactSummary() {
        return String.format("%s (%s) | Phone: %s | Email: %s", name, getRole(), phone, email);
    }

    // Getters and Setters with Encapsulation & Validation
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        if (age < 0 || age > 130) {
            throw new IllegalArgumentException("Invalid age: " + age + ". Age must be between 0 and 130.");
        }
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @Override
    public String toString() {
        return String.format("[%s] ID: %s | Name: %s | Age: %d | Gender: %s", getRole(), id, name, age, gender);
    }
}
