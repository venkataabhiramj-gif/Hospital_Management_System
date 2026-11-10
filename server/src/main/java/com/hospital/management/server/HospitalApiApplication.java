package com.hospital.management.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HospitalApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(HospitalApiApplication.class, args);
    }
}
