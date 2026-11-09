package com.hospital.models;

import com.hospital.interfaces.Billable;
import com.hospital.exceptions.InvalidBillingException;

/**
 * Bill entity representing medical invoice charges.
 * Demonstrates:
 *  1. Implementation of Billable interface
 *  2. Encapsulation & financial calculation business logic
 *  3. Throwing custom exception (InvalidBillingException) for domain validation
 */
public class Bill implements Billable {
    private int billId;
    private String patientId;
    private String patientName;
    private double consultationFee;
    private double treatmentCharges;
    private double medicineCharges;
    private double discountPercent; // e.g., 5.0 for 5%
    private double taxPercent;      // e.g., 18.0 for 18% GST
    private double totalAmount;
    private String paymentStatus;   // PENDING, PAID
    private String billingDate;

    public Bill() {
    }

    public Bill(int billId, String patientId, String patientName, double consultationFee,
                double treatmentCharges, double medicineCharges, double discountPercent,
                double taxPercent, String paymentStatus, String billingDate) throws InvalidBillingException {
        this.billId = billId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.consultationFee = consultationFee;
        this.treatmentCharges = treatmentCharges;
        this.medicineCharges = medicineCharges;
        this.discountPercent = discountPercent;
        this.taxPercent = taxPercent;
        this.paymentStatus = paymentStatus;
        this.billingDate = billingDate;
        this.totalAmount = calculateFinalTotal();
    }

    // --- Interface Implementation (Billable) ---

    @Override
    public double calculateSubtotal() {
        return consultationFee + treatmentCharges + medicineCharges;
    }

    @Override
    public double calculateDiscountAmount() {
        return (calculateSubtotal() * discountPercent) / 100.0;
    }

    @Override
    public double calculateTaxAmount() {
        double discountedSubtotal = calculateSubtotal() - calculateDiscountAmount();
        return (discountedSubtotal * taxPercent) / 100.0;
    }

    @Override
    public double calculateFinalTotal() throws InvalidBillingException {
        if (consultationFee < 0 || treatmentCharges < 0 || medicineCharges < 0) {
            throw new InvalidBillingException("Billing charges cannot be negative numbers.");
        }
        if (discountPercent < 0 || discountPercent > 100) {
            throw new InvalidBillingException("Discount percentage must be between 0% and 100%.");
        }
        if (taxPercent < 0) {
            throw new InvalidBillingException("Tax percentage cannot be negative.");
        }

        double discounted = calculateSubtotal() - calculateDiscountAmount();
        double finalTotal = discounted + calculateTaxAmount();
        return Math.round(finalTotal * 100.0) / 100.0; // round to 2 decimal places
    }

    // Getters and Setters
    public int getBillId() {
        return billId;
    }

    public void setBillId(int billId) {
        this.billId = billId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public double getTreatmentCharges() {
        return treatmentCharges;
    }

    public void setTreatmentCharges(double treatmentCharges) {
        this.treatmentCharges = treatmentCharges;
    }

    public double getMedicineCharges() {
        return medicineCharges;
    }

    public void setMedicineCharges(double medicineCharges) {
        this.medicineCharges = medicineCharges;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public double getTaxPercent() {
        return taxPercent;
    }

    public void setTaxPercent(double taxPercent) {
        this.taxPercent = taxPercent;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getBillingDate() {
        return billingDate;
    }

    public void setBillingDate(String billingDate) {
        this.billingDate = billingDate;
    }
}
