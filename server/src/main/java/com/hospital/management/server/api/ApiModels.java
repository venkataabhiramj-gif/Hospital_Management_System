package com.hospital.management.server.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;

public final class ApiModels {
    private ApiModels() {
    }

    public record PatientCreateRequest(
            @NotBlank @Size(max = 200) String fullName,
            LocalDate dateOfBirth,
            @Size(max = 32) String phone) {
    }

    public record PatientUpdateRequest(
            @NotBlank @Size(max = 200) String fullName,
            LocalDate dateOfBirth,
            @Size(max = 32) String phone) {
    }

    public record PatientResponse(
            long id,
            String patientNumber,
            String fullName,
            LocalDate dateOfBirth,
            String phone,
            boolean active,
            OffsetDateTime createdAt) {
    }

    public record StaffCreateRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(min = 12, max = 128) String password,
            @NotBlank @Size(max = 200) String displayName,
            @NotBlank String role) {
    }

    public record StaffResponse(long id, String username, String displayName, String role, boolean active) {
    }

    public record DoctorCreateRequest(
            @NotNull Long staffUserId,
            @NotBlank @Size(max = 120) String specialty,
            @Min(5) Integer appointmentMinutes) {
    }

    public record DoctorResponse(long id, long staffUserId, String displayName, String specialty,
                                 int appointmentMinutes, boolean active) {
    }

    public record ScheduleRequest(
            @Min(1) @NotNull Integer dayOfWeek,
            @NotNull LocalTime startsAt,
            @NotNull LocalTime endsAt) {
    }

    public record ScheduleResponse(long id, int dayOfWeek, LocalTime startsAt, LocalTime endsAt) {
    }

    public record AppointmentCreateRequest(
            @NotBlank String patientNumber,
            @NotNull Long doctorId,
            @NotNull OffsetDateTime startsAt,
            @NotNull OffsetDateTime endsAt,
            @NotBlank String visitType,
            @Size(max = 500) String reason) {
    }

    public record AppointmentStatusRequest(@NotBlank String status) {
    }

    public record AppointmentResponse(
            long id,
            String patientNumber,
            String patientName,
            long doctorId,
            String doctorName,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String visitType,
            String status,
            String reason) {
    }

    public record EncounterStartRequest(@NotNull Long appointmentId) {
    }

    public record EncounterUpdateRequest(
            @Size(max = 1000) String complaint,
            @Size(max = 20) String bloodPressure,
            @Min(0) Integer pulsePerMinute,
            @DecimalMin("20.0") BigDecimal temperatureCelsius,
            @DecimalMin("0.0") BigDecimal weightKg,
            String allergies,
            String currentMedicines,
            String clinicalNotes,
            @Size(max = 1000) String diagnosisText) {
    }

    public record EncounterAddendumRequest(
            @NotBlank @Size(max = 500) String correctionReason,
            @NotBlank String addendumText) {
    }

    public record EncounterResponse(
            long id,
            long appointmentId,
            String patientNumber,
            String patientName,
            String doctorName,
            String complaint,
            String bloodPressure,
            Integer pulsePerMinute,
            BigDecimal temperatureCelsius,
            BigDecimal weightKg,
            String allergies,
            String currentMedicines,
            String clinicalNotes,
            String diagnosisText,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime finalizedAt) {
    }

    public record MedicineCreateRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 200) String genericName,
            @Size(max = 200) String brandName,
            @Size(max = 80) String strength,
            @Size(max = 80) String dosageForm,
            @NotBlank @Size(max = 40) String unit,
            @Min(0) int reorderLevel,
            @NotNull @DecimalMin("0.00") BigDecimal salePrice) {
    }

    public record MedicineResponse(long id, String code, String genericName, String brandName,
                                   String strength, String dosageForm, String unit, int reorderLevel,
                                   BigDecimal salePrice, int quantityOnHand, boolean active) {
    }

    public record StockReceiptRequest(
            @NotNull Long medicineId,
            @NotBlank @Size(max = 100) String batchNumber,
            @NotNull LocalDate expiryDate,
            @Size(max = 200) String supplier,
            @NotNull @DecimalMin("0.00") BigDecimal purchasePrice,
            @Min(1) int quantity) {
    }

    public record StockAdjustmentRequest(
            @NotNull Long batchId,
            @NotNull Integer quantityChange,
            @NotBlank @Size(max = 500) String reason) {
    }

    public record PrescriptionItemRequest(
            @NotNull Long medicineId,
            @NotBlank @Size(max = 120) String dosage,
            @Size(max = 80) String route,
            @NotBlank @Size(max = 120) String frequency,
            @Min(1) Integer durationDays,
            @Min(1) int quantity,
            @Size(max = 1000) String instructions) {
    }

    public record PrescriptionCreateRequest(
            @NotNull Long encounterId,
            @NotNull java.util.List<PrescriptionItemRequest> items) {
    }

    public record PrescriptionItemResponse(long id, long medicineId, String medicineName, String dosage,
                                           String route, String frequency, Integer durationDays,
                                           int quantityPrescribed, int quantityDispensed, String instructions) {
    }

    public record DispenseRequest(
            @NotNull Long prescriptionItemId,
            @NotNull @Min(1) Integer quantity) {
    }

    public record ServiceCreateRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 200) String description,
            @NotNull @DecimalMin("0.00") BigDecimal unitPrice) {
    }

    public record ServiceResponse(long id, String code, String description, BigDecimal unitPrice, boolean active) {
    }

    public record InvoiceResponse(long id, String invoiceNumber, String patientNumber, String patientName,
                                  String status, BigDecimal totalAmount, BigDecimal amountPaid,
                                  OffsetDateTime createdAt) {
    }

    public record PaymentRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String method) {
    }

    public record PaymentResponse(long id, String receiptNumber, long invoiceId, BigDecimal amount,
                                  String method, OffsetDateTime receivedAt) {
    }

    public record VoidInvoiceRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record RefundRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Size(max = 500) String reason) {
    }

    public record ApiError(OffsetDateTime timestamp, int status, String error) {
    }
}
