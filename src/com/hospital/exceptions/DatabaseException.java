package com.hospital.exceptions;

/**
 * Thrown when an underlying database (JDBC / SQL) failure occurs.
 * Encapsulates lower-level SQLException to keep data layer independent.
 */
public class DatabaseException extends HospitalException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseException(String message) {
        super(message);
    }
}
