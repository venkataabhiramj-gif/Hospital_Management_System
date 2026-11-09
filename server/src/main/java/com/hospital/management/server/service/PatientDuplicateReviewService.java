package com.hospital.management.server.service;

import java.time.LocalDate;
import java.util.List;

/** Finds records staff should compare before adding or correcting a patient. */
public interface PatientDuplicateReviewService {
    String normalizePhone(String phone);

    List<PatientDuplicateMatch> findPossibleMatches(
            String fullName,
            LocalDate dateOfBirth,
            String phone,
            Long excludePatientId);
}
