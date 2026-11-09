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
        String username = authentication != null ? authentication.getName() : null;
        Long actorId = null;
        if (username != null && !"anonymousUser".equalsIgnoreCase(username)) {
            java.util.List<Long> ids = jdbc.queryForList("SELECT id FROM staff_users WHERE LOWER(username) = ?", Long.class, username.toLowerCase(java.util.Locale.ROOT));
            if (!ids.isEmpty()) actorId = ids.get(0);
        }
        if (actorId == null) {
            java.util.List<Long> fallback = jdbc.queryForList("SELECT id FROM staff_users WHERE active=TRUE ORDER BY CASE WHEN role='ADMIN' THEN 1 WHEN role='RECEPTIONIST' THEN 2 ELSE 3 END, id LIMIT 1", Long.class);
            actorId = fallback.isEmpty() ? 1L : fallback.get(0);
        }
        jdbc.update("INSERT INTO audit_events (actor_id, action, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)",
                actorId, action, entityType, String.valueOf(entityId), details);
    }
}
