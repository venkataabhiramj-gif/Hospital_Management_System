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
                        .requestMatchers("/", "/index.html", "/*.html", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/api/health", "/api/dashboard").permitAll()
                        .requestMatchers("/api/bills", "/api/bills/**").permitAll()
                        .requestMatchers("/api/records", "/api/records/**").permitAll()
                        .requestMatchers("/api/patients", "/api/patients/**").permitAll()
                        .requestMatchers("/api/doctors", "/api/doctors/**").permitAll()
                        .requestMatchers("/api/appointments", "/api/appointments/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/patients/*/demographic-history").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/patients/*/duplicate-review").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/patients/*/encounters").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.GET, "/api/encounters/*/prescriptions").hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST")
                        .requestMatchers(HttpMethod.POST, "/api/prescription-items/*/substitution-approvals").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/prescription-items/*/dispense").hasAnyRole("ADMIN", "PHARMACIST")
                        .requestMatchers(HttpMethod.GET, "/api/prescription-items/*/substitution-history").hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST")
                        .requestMatchers("/api/encounters/**").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/prescriptions").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers("/api/prescription-items/**").hasAnyRole("ADMIN", "PHARMACIST")
                        .requestMatchers("/api/prescriptions/**").hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST")
                        .requestMatchers("/api/inventory/**").hasAnyRole("ADMIN", "PHARMACIST")
                        .requestMatchers("/api/billing/**").hasAnyRole("ADMIN", "BILLING")
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
