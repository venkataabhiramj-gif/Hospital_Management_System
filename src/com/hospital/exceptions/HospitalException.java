package com.hospital.exceptions;

/**
 * Base checked exception for all domain-specific errors in the Hospital Management System.
 * Academic Concept: Exception Handling Hierarchy (Polymorphic error handling).
 */
public class HospitalException extends Exception {
    public HospitalException(String message) {
        super(message);
    }

    public HospitalException(String message, Throwable cause) {
        super(message, cause);
    }
}
