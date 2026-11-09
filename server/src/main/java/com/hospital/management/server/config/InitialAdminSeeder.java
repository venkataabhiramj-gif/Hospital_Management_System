package com.hospital.management.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

@Component
public class InitialAdminSeeder implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(InitialAdminSeeder.class);
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final String username;
    private final String configuredPassword;

    public InitialAdminSeeder(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, Environment environment,
                              @Value("${hms.bootstrap.username:admin}") String username,
                              @Value("${hms.bootstrap.password:}") String configuredPassword) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.username = username.trim().toLowerCase(Locale.ROOT);
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM staff_users", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        String password = configuredPassword;
        if (password == null || password.isBlank()) {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("Set HMS_ADMIN_PASSWORD before starting an empty production database.");
            }
            password = generatePassword();
            logger.warn("DEV SETUP: initial admin username is '{}'; one-time password is {}", username, password);
            logger.warn("Set HMS_ADMIN_PASSWORD to choose the initial admin password on a fresh development database.");
        }
        if (password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("The initial administrator password must be at least 12 characters and no more than 72 UTF-8 bytes.");
        }
        jdbc.update("INSERT INTO staff_users (username, password_hash, display_name, role) VALUES (?, ?, ?, 'ADMIN')",
                username, passwordEncoder.encode(password), "System Administrator");
    }

    private String generatePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(24);
        for (int index = 0; index < 24; index++) {
            password.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }
}
