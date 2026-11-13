package com.hospital.management.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Locale;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 1. Static web assets & public gateway endpoints
                        .requestMatchers(
                                "/", "/login.html", "/index.html", "/*.html",
                                "/css/**", "/js/**", "/downloads/**",
                                "/api/download/**", "/favicon.ico", "/error"
                        ).permitAll()
                        .requestMatchers("/api/health", "/api/dashboard", "/api/auth/**").permitAll()

                        // 2. Patient self-service portal & public doctor directory
                        .requestMatchers("/api/patient/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/doctors", "/api/doctors/**").permitAll()

                        // 3. Administrative-only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/automation/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/doctors/web").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/patients").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/records").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/patients/*/demographic-history").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/patients/*/duplicate-review").hasRole("ADMIN")

                        // 4. Clinical encounters & medical records (Doctors & Admin)
                        .requestMatchers(HttpMethod.GET, "/api/patients/*/encounters").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/records").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers("/api/records", "/api/records/**").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers("/api/encounters/**").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers("/api/doctor/**").hasAnyRole("ADMIN", "DOCTOR")

                        // 5. Prescriptions & Pharmacy (Doctors, Pharmacists, Admin)
                        .requestMatchers(HttpMethod.POST, "/api/prescriptions").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/prescription-items/*/substitution-approvals").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/prescription-items/*/dispense").hasAnyRole("ADMIN", "PHARMACIST")
                        .requestMatchers(HttpMethod.GET, "/api/prescription-items/*/substitution-history").hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST")
                        .requestMatchers(HttpMethod.GET, "/api/encounters/*/prescriptions").hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST")
                        .requestMatchers("/api/prescription-items/**").hasAnyRole("ADMIN", "PHARMACIST")
                        .requestMatchers("/api/prescriptions/**").hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST")
                        .requestMatchers("/api/inventory/**").hasAnyRole("ADMIN", "PHARMACIST")

                        // 6. Patient registrations & demographic updates (Receptionists & Admin)
                        .requestMatchers(HttpMethod.PUT, "/api/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.POST, "/api/patients").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.GET, "/api/patients", "/api/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR", "BILLING")

                        // 7. Appointments & Scheduling (Receptionists, Doctors, Admin)
                        .requestMatchers(HttpMethod.DELETE, "/api/appointments").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers("/api/appointments", "/api/appointments/**").hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR")

                        // 8. Billing & Invoices (Billing staff & Admin)
                        .requestMatchers("/api/bills", "/api/bills/**", "/api/billing/**").hasAnyRole("ADMIN", "BILLING", "RECEPTIONIST")

                        // 9. All other requests require authentication
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(JdbcTemplate jdbc) {
        return username -> {
            List<User> users = jdbc.query(
                    "SELECT username, password_hash, role, active FROM staff_users WHERE username = ?",
                    (resultSet, rowNumber) -> new User(
                            resultSet.getString("username"),
                            resultSet.getString("password_hash"),
                            resultSet.getBoolean("active"),
                            true,
                            true,
                            true,
                            List.of(new SimpleGrantedAuthority("ROLE_" + resultSet.getString("role")))),
                    username.toLowerCase(Locale.ROOT));
            if (users.isEmpty()) {
                throw new UsernameNotFoundException("Invalid username or password");
            }
            return users.get(0);
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
