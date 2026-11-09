package com.hospital.models;

/**
 * Concrete Patient model representing a registered care recipient.
 * Demonstrates:
 *  1. Inheritance (extends abstract Person)
 *  2. Polymorphism (overrides getRole(), toString())
 *  3. Encapsulation (patient-specific fields like bloodGroup, address)
 */
public class Patient extends Person {
    private String bloodGroup;
    private String address;
    private String emergencyContact;
    private String registeredDate;

    public Patient() {
        super();
    }

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

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(String registeredDate) {
        this.registeredDate = registeredDate;
    }

    @Override
    public String toString() {
        return String.format("%s | Blood Group: %s | Contact: %s", super.toString(), bloodGroup, phone);
    }
}
