package com.hospital.exceptions;

/**
 * Thrown when billing calculations or financial amounts violate business rules
 * (e.g., negative charges, invalid tax rate, discount exceeding 100%).
 */
public class InvalidBillingException extends HospitalException {
    public InvalidBillingException(String message) {
        super(message);
    }
}
