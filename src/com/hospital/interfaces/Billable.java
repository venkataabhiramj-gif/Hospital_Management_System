package com.hospital.interfaces;

import com.hospital.exceptions.InvalidBillingException;

/**
 * Interface defining the financial contract for anything that can be billed.
 * Academic Concept: Java Interfaces & Contract-driven design.
 */
public interface Billable {
    double calculateSubtotal();
    double calculateDiscountAmount();
    double calculateTaxAmount();
    double calculateFinalTotal() throws InvalidBillingException;
}
