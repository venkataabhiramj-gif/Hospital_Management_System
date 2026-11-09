package com.hospital.management.server.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;

    public AuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(String action, String entityType, Object entityId, String details) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("An authenticated staff account is required for audited changes");
        }
        Long actorId = jdbc.queryForObject(
                "SELECT id FROM staff_users WHERE username = ?", Long.class, authentication.getName());
        jdbc.update("INSERT INTO audit_events (actor_id, action, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)",
                actorId, action, entityType, String.valueOf(entityId), details);
    }
}
