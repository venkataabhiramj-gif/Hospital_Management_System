package com.hospital;

import com.hospital.config.DatabaseConfig;
import com.hospital.web.ApiHandler;
import com.hospital.web.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Hospital Management System (HMS) - Web Application Entry Point.
 * Academic Objectives:
 *  - Core OOP (Inheritance, Polymorphism, Encapsulation, Abstraction)
 *  - Custom Exception Handling Hierarchy
 *  - Interface Contracts & DAO Architecture
 *  - Secure JDBC Database Connectivity (PreparedStatement)
 *  - Embedded Lightweight Java HTTP Web Server
 */
public class Main {
    private static final int DEFAULT_PORT = 8088;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("============================================================");
        System.out.println("   HOSPITAL MANAGEMENT SYSTEM (HMS) - JAVA WEB PORTAL       ");
        System.out.println("   SRM University AP - CSE Java Mini Project / Assignment    ");
        System.out.println("============================================================");

        try {
            // 1. Initialize SQLite Database & Tables
            System.out.println("[1/3] Initializing Database & Seed Records...");
            DatabaseConfig.getInstance();

            // 2. Locate webapp static directory
            String webappDir = "webapp";
            if (!new File(webappDir).exists() && new File("../webapp").exists()) {
                webappDir = "../webapp";
            }
            System.out.println("[2/3] Web Assets Directory: " + new File(webappDir).getAbsolutePath());

            // 3. Create and configure HTTP Server
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Register API routes and Static UI routes
            ApiHandler apiHandler = new ApiHandler();
            server.createContext("/api/", apiHandler);
            server.createContext("/", new StaticFileHandler(webappDir));

            // Set multi-threaded concurrent request executor
            server.setExecutor(Executors.newFixedThreadPool(12));
            server.start();

            System.out.println("[3/3] Server started successfully!");
            System.out.println("------------------------------------------------------------");
            System.out.println(">> OPEN IN BROWSER: http://localhost:" + port);
            System.out.println("------------------------------------------------------------");
            System.out.println("Active Modules:");
            System.out.println("  [+] 1. Patient Registration & Search");
            System.out.println("  [+] 2. Doctor Directory & Management");
            System.out.println("  [+] 3. Appointment Booking & Conflict Check");
            System.out.println("  [+] 4. Billing & Automated Invoicing (Billable Interface)");
            System.out.println("  [+] 5. Medical Records & Clinical Consultations");
            System.out.println("============================================================");
            System.out.println("Press Ctrl+C in this terminal to stop the server.");

        } catch (IOException e) {
            System.err.println("Failed to start HTTP server on port " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
