package com.hospital.exceptions;

/**
 * Thrown when an operation queries or assigns a doctor that does not exist in the system.
 */
public class DoctorNotFoundException extends HospitalException {
    private final String doctorId;

    public DoctorNotFoundException(String doctorId) {
        super("Doctor not found with ID: " + doctorId);
        this.doctorId = doctorId;
    }

    public String getDoctorId() {
        return doctorId;
    }
}
