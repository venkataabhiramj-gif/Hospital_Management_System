package com.hospital.management.server.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** JDBC implementation of patient matching for the relational clinic database. */
@Service
public class JdbcPatientDuplicateReviewService implements PatientDuplicateReviewService {
    private final JdbcTemplate db;

    public JdbcPatientDuplicateReviewService(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    @Override
    public List<PatientDuplicateMatch> findPossibleMatches(
            String fullName,
            LocalDate dateOfBirth,
            String rawPhone,
            Long excludePatientId) {
        String name = fullName.trim().toLowerCase(Locale.ROOT);
        String phone = normalizePhone(rawPhone);
        List<Map<String, Object>> candidates;
        if (excludePatientId == null) {
            candidates = db.queryForList("""
                    SELECT id,patient_number,full_name,date_of_birth,phone,phone_match_key
                    FROM patients
                    WHERE active=TRUE
                      AND (LOWER(full_name)=? OR (?<>'' AND phone_match_key=?))
                    ORDER BY full_name
                    LIMIT 100
                    """, name, phone, phone);
        } else {
            candidates = db.queryForList("""
                    SELECT id,patient_number,full_name,date_of_birth,phone,phone_match_key
                    FROM patients
                    WHERE active=TRUE AND id<>?
                      AND (LOWER(full_name)=? OR (?<>'' AND phone_match_key=?))
                    ORDER BY full_name
                    LIMIT 100
                    """, excludePatientId, name, phone, phone);
        }

        List<PatientDuplicateMatch> matches = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            boolean sameName = name.equals(String.valueOf(candidate.get("full_name")).trim().toLowerCase(Locale.ROOT));
            boolean samePhone = !phone.isBlank() && phone.equals(candidate.get("phone_match_key"));
            LocalDate candidateDateOfBirth = toLocalDate(candidate.get("date_of_birth"));
            boolean sameDateOfBirth = dateOfBirth != null && dateOfBirth.equals(candidateDateOfBirth);

            List<String> signals = new ArrayList<>();
            if (samePhone) signals.add("PHONE_MATCH");
            if (sameName) signals.add("NAME_MATCH");
            if (sameDateOfBirth) signals.add("DATE_OF_BIRTH_MATCH");
            if (signals.isEmpty()) continue;

            matches.add(new PatientDuplicateMatch(
                    ((Number) candidate.get("id")).longValue(),
                    (String) candidate.get("patient_number"),
                    (String) candidate.get("full_name"),
                    candidateDateOfBirth,
                    (String) candidate.get("phone"),
                    String.join(", ", signals),
                    samePhone || (sameName && sameDateOfBirth) ? "HIGH" : "REVIEW"));
        }
        return matches;
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate date) return date;
        if (value instanceof Date date) return date.toLocalDate();
        return LocalDate.parse(value.toString());
    }
}
