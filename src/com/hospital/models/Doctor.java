package com.hospital.models;

/**
 * Concrete Doctor model representing medical practitioners.
 * Demonstrates:
 *  1. Inheritance (extends abstract Person)
 *  2. Polymorphism (overrides getRole(), toString())
 *  3. Encapsulation (validated consultationFee)
 */
public class Doctor extends Person {
    private String specialization;
    private String qualification;
    private double consultationFee;
    private String availableDays;
    private String availableHours;

    public Doctor() {
        super();
    }

    public Doctor(String id, String name, String email, String phone, int age, String gender,
                  String specialization, String qualification, double consultationFee,
                  String availableDays, String availableHours) {
        super(id, name, email, phone, age, gender);
        this.specialization = specialization;
        this.qualification = qualification;
        setConsultationFee(consultationFee);
        this.availableDays = availableDays;
        this.availableHours = availableHours;
    }

    @Override
    public String getRole() {
        return "DOCTOR";
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        if (consultationFee < 0) {
            throw new IllegalArgumentException("Consultation fee cannot be negative.");
        }
        this.consultationFee = consultationFee;
    }

    public String getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(String availableDays) {
        this.availableDays = availableDays;
    }

    public String getAvailableHours() {
        return availableHours;
    }

    public void setAvailableHours(String availableHours) {
        this.availableHours = availableHours;
    }

    @Override
    public String toString() {
        return String.format("%s | Specialty: %s | Fee: ₹%.2f", super.toString(), specialization, consultationFee);
    }
}
