package com.hospital.management.learning;

import com.hospital.management.model.Patient;

public class PatientDemo {
    public static void main(String[] args) {
        Patient patient = new Patient("PAT-0001", "Demo Patient");

        System.out.println("Patient ID: " + patient.getPatientId());
        System.out.println("Name: " + patient.getFullName());

        try {
            new Patient(" ", "Demo Patient");
        } catch (IllegalArgumentException exception) {
            System.out.println("Patient was not created: " + exception.getMessage());
        }
    }
}
