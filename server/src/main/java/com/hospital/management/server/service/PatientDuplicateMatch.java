package com.hospital.management.server.service;

import java.time.LocalDate;

/** A possible existing patient and the details that caused the match. */
public record PatientDuplicateMatch(
        long id,
        String patientNumber,
        String fullName,
        LocalDate dateOfBirth,
        String phone,
        String matchSignals,
        String confidence) {
}
