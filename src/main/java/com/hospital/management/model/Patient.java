package com.hospital.management.model;

public class Patient {
    private final String patientId;
    private final String fullName;

    public Patient(String patientId, String fullName) {
        if (patientId == null || patientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Patient ID cannot be blank.");
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Patient name cannot be blank.");
        }

        this.patientId = patientId.trim();
        this.fullName = fullName.trim();
    }

    public String getPatientId() {
        return patientId;
    }

    public String getFullName() {
        return fullName;
    }
}
