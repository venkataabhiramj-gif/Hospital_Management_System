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
 * ApexCare Clinical EHR & Hospital Management System.
 * Outpatient Clinical OS & Hospital Administrative Service Entry Point.
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

        System.out.println("=====================================================================");
        System.out.println("   APEXCARE CLINICAL EHR & HEALTHCARE INFORMATION SYSTEM (HIS)       ");
        System.out.println("   Enterprise Outpatient Operations & Revenue Management Platform    ");
        System.out.println("=====================================================================");

        try {
            // 1. Initialize Relational Database Engine
            System.out.println("[Core] Initializing Clinical Database Engine (SQLite/JDBC)...");
            DatabaseConfig.getInstance();

            // 2. Resolve Static Assets
            String webappDir = "webapp";
            if (!new File(webappDir).exists() && new File("../webapp").exists()) {
                webappDir = "../webapp";
            }
            System.out.println("[Core] Static Web Assets Path: " + new File(webappDir).getAbsolutePath());

            // 3. Configure HTTP Gateway & Routing
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            ApiHandler apiHandler = new ApiHandler();
            server.createContext("/api/", apiHandler);
            server.createContext("/", new StaticFileHandler(webappDir));

            // Multi-threaded concurrent connection pool
            server.setExecutor(Executors.newFixedThreadPool(16));
            server.start();

            System.out.println("[Core] Enterprise HTTP Gateway online and listening on port " + port);
            System.out.println("---------------------------------------------------------------------");
            System.out.println(">> PORTAL ACCESS: http://localhost:" + port);
            System.out.println("---------------------------------------------------------------------");
            System.out.println("Active Clinical Subsystems:");
            System.out.println("  * Outpatient Registration & Master Patient Index (MPI)");
            System.out.println("  * Medical Staff Directory & Specialty Scheduling");
            System.out.println("  * Clinical Encounter Booking & Double-Booking Prevention");
            System.out.println("  * Revenue Cycle Management (RCM) & Point-of-Care Billing");
            System.out.println("  * Electronic Health Records (EHR) & Clinical Consultation Notes");
            System.out.println("=====================================================================");
            System.out.println("Press Ctrl+C in this console to safely shut down the service.");

        } catch (IOException e) {
            System.err.println("[FATAL] Failed to initialize HTTP Gateway on port " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
