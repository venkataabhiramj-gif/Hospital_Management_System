package com.hospital.exceptions;

/**
 * Thrown when an operation refers to a patient identifier that does not exist in the database.
 */
public class PatientNotFoundException extends HospitalException {
    private final String patientId;

    public PatientNotFoundException(String patientId) {
        super("Patient not found with ID: " + patientId);
        this.patientId = patientId;
    }

    public String getPatientId() {
        return patientId;
    }
}
