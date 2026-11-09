package com.hospital.web;

import com.hospital.dao.*;
import com.hospital.exceptions.*;
import com.hospital.interfaces.*;
import com.hospital.models.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central REST API & Form Submission Controller.
 * Handles HTTP requests, invokes DAO layers, and translates domain exceptions into standard HTTP responses.
 */
public class ApiHandler implements HttpHandler {
    private final PatientDAO patientDAO = new PatientDAOImpl();
    private final DoctorDAO doctorDAO = new DoctorDAOImpl();
    private final AppointmentDAO appointmentDAO = new AppointmentDAOImpl();
    private final BillingDAO billingDAO = new BillingDAOImpl();
    private final MedicalRecordDAO recordDAO = new MedicalRecordDAOImpl();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();
        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());

        // Set CORS & JSON headers
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            switch (path) {
                case "/api/dashboard":
                    handleDashboard(exchange);
                    break;
                case "/api/patients":
                    handlePatients(exchange, method, queryParams);
                    break;
                case "/api/doctors":
                    handleDoctors(exchange, method, queryParams);
                    break;
                case "/api/appointments":
                    handleAppointments(exchange, method, queryParams);
                    break;
                case "/api/bills":
                    handleBills(exchange, method, queryParams);
                    break;
                case "/api/records":
                    handleRecords(exchange, method, queryParams);
                    break;
                default:
                    sendResponse(exchange, 404, JsonUtils.errorJson("Endpoint not found: " + path));
                    break;
            }
        } catch (PatientNotFoundException | DoctorNotFoundException e) {
            sendResponse(exchange, 404, JsonUtils.errorJson(e.getMessage()));
        } catch (AppointmentConflictException e) {
            sendResponse(exchange, 409, JsonUtils.errorJson(e.getMessage()));
        } catch (InvalidBillingException e) {
            sendResponse(exchange, 400, JsonUtils.errorJson(e.getMessage()));
        } catch (DatabaseException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, JsonUtils.errorJson("Database error: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, JsonUtils.errorJson("Internal server error: " + e.getMessage()));
        }
    }

    // --- Module 1: Dashboard Analytics ---
    private void handleDashboard(HttpExchange exchange) throws Exception {
        int patientCount = patientDAO.getPatientCount();
        int doctorCount = doctorDAO.getDoctorCount();
        int appointmentCount = appointmentDAO.getAppointmentCount();
        double revenue = billingDAO.getTotalRevenue();

        String json = String.format(
                "{\"patients\":%d,\"doctors\":%d,\"appointments\":%d,\"revenue\":%.2f}",
                patientCount, doctorCount, appointmentCount, revenue
        );
        sendResponse(exchange, 200, json);
    }

    // --- Module 2: Patients ---
    private void handlePatients(HttpExchange exchange, String method, Map<String, String> query) throws Exception {
        if ("GET".equals(method)) {
            String search = query.get("q");
            List<Patient> list = (search != null && !search.trim().isEmpty())
                    ? patientDAO.searchPatients(search)
                    : patientDAO.getAllPatients();
            sendResponse(exchange, 200, JsonUtils.patientListToJson(list));
        } else if ("POST".equals(method)) {
            Map<String, String> body = parseRequestBody(exchange);
            String id = body.getOrDefault("id", "PAT-" + System.currentTimeMillis() % 10000);
            Patient p = new Patient(
                    id,
                    body.get("name"),
                    body.get("email"),
                    body.get("phone"),
                    Integer.parseInt(body.getOrDefault("age", "0")),
                    body.get("gender"),
                    body.get("bloodGroup"),
                    body.get("address"),
                    body.get("emergencyContact"),
                    body.getOrDefault("registeredDate", LocalDate.now().toString())
            );
            patientDAO.addPatient(p);
            sendResponse(exchange, 201, JsonUtils.successJson("Patient registered successfully with ID: " + id));
        } else if ("DELETE".equals(method)) {
            String id = query.get("id");
            if (id == null) {
                sendResponse(exchange, 400, JsonUtils.errorJson("Patient ID is required for deletion."));
                return;
            }
            patientDAO.deletePatient(id);
            sendResponse(exchange, 200, JsonUtils.successJson("Patient deleted successfully."));
        }
    }

    // --- Module 3: Doctors ---
    private void handleDoctors(HttpExchange exchange, String method, Map<String, String> query) throws Exception {
        if ("GET".equals(method)) {
            String spec = query.get("spec");
            List<Doctor> list = (spec != null && !spec.trim().isEmpty())
                    ? doctorDAO.getDoctorsBySpecialization(spec)
                    : doctorDAO.getAllDoctors();
            sendResponse(exchange, 200, JsonUtils.doctorListToJson(list));
        } else if ("POST".equals(method)) {
            Map<String, String> body = parseRequestBody(exchange);
            String id = body.getOrDefault("id", "DOC-" + (100 + doctorDAO.getDoctorCount() + 1));
            Doctor d = new Doctor(
                    id,
                    body.get("name"),
                    body.get("email"),
                    body.get("phone"),
                    Integer.parseInt(body.getOrDefault("age", "35")),
                    body.get("gender"),
                    body.get("specialization"),
                    body.get("qualification"),
                    Double.parseDouble(body.getOrDefault("consultationFee", "500")),
                    body.get("availableDays"),
                    body.get("availableHours")
            );
            doctorDAO.addDoctor(d);
            sendResponse(exchange, 201, JsonUtils.successJson("Doctor added successfully with ID: " + id));
        } else if ("DELETE".equals(method)) {
            String id = query.get("id");
            if (id == null) {
                sendResponse(exchange, 400, JsonUtils.errorJson("Doctor ID is required."));
                return;
            }
            doctorDAO.deleteDoctor(id);
            sendResponse(exchange, 200, JsonUtils.successJson("Doctor removed successfully."));
        }
    }

    // --- Module 4: Appointments ---
    private void handleAppointments(HttpExchange exchange, String method, Map<String, String> query) throws Exception {
        if ("GET".equals(method)) {
            String doctorId = query.get("doctorId");
            String patientId = query.get("patientId");
            List<Appointment> list;
            if (doctorId != null) {
                list = appointmentDAO.getAppointmentsByDoctor(doctorId);
            } else if (patientId != null) {
                list = appointmentDAO.getAppointmentsByPatient(patientId);
            } else {
                list = appointmentDAO.getAllAppointments();
            }
            sendResponse(exchange, 200, JsonUtils.appointmentListToJson(list));
        } else if ("POST".equals(method)) {
            Map<String, String> body = parseRequestBody(exchange);
            String action = body.get("action");

            if ("updateStatus".equalsIgnoreCase(action)) {
                int id = Integer.parseInt(body.get("id"));
                String status = body.get("status");
                appointmentDAO.updateAppointmentStatus(id, status);
                sendResponse(exchange, 200, JsonUtils.successJson("Appointment status updated to: " + status));
            } else {
                Appointment a = new Appointment(
                        0,
                        body.get("patientId"),
                        body.get("patientName"),
                        body.get("doctorId"),
                        body.get("doctorName"),
                        body.get("appointmentDate"),
                        body.get("timeSlot"),
                        "BOOKED",
                        body.get("reason")
                );
                appointmentDAO.bookAppointment(a);
                sendResponse(exchange, 201, JsonUtils.successJson("Appointment booked successfully."));
            }
        }
    }

    // --- Module 5: Billing System ---
    private void handleBills(HttpExchange exchange, String method, Map<String, String> query) throws Exception {
        if ("GET".equals(method)) {
            String patientId = query.get("patientId");
            List<Bill> list = (patientId != null)
                    ? billingDAO.getBillsByPatient(patientId)
                    : billingDAO.getAllBills();
            sendResponse(exchange, 200, JsonUtils.billListToJson(list));
        } else if ("POST".equals(method)) {
            Map<String, String> body = parseRequestBody(exchange);
            String action = body.get("action");

            if ("markPaid".equalsIgnoreCase(action)) {
                int billId = Integer.parseInt(body.get("billId"));
                billingDAO.updatePaymentStatus(billId, "PAID");
                sendResponse(exchange, 200, JsonUtils.successJson("Bill marked as PAID."));
            } else {
                Bill bill = new Bill(
                        0,
                        body.get("patientId"),
                        body.get("patientName"),
                        Double.parseDouble(body.getOrDefault("consultationFee", "0")),
                        Double.parseDouble(body.getOrDefault("treatmentCharges", "0")),
                        Double.parseDouble(body.getOrDefault("medicineCharges", "0")),
                        Double.parseDouble(body.getOrDefault("discountPercent", "0")),
                        Double.parseDouble(body.getOrDefault("taxPercent", "5")),
                        body.getOrDefault("paymentStatus", "PENDING"),
                        body.getOrDefault("billingDate", LocalDate.now().toString())
                );
                billingDAO.createBill(bill);
                sendResponse(exchange, 201, JsonUtils.successJson("Invoice generated successfully. Total: ₹" + bill.getTotalAmount()));
            }
        }
    }

    // --- Module 6: Medical Records ---
    private void handleRecords(HttpExchange exchange, String method, Map<String, String> query) throws Exception {
        if ("GET".equals(method)) {
            String patientId = query.get("patientId");
            List<MedicalRecord> list = (patientId != null)
                    ? recordDAO.getRecordsByPatient(patientId)
                    : recordDAO.getAllRecords();
            sendResponse(exchange, 200, JsonUtils.recordListToJson(list));
        } else if ("POST".equals(method)) {
            Map<String, String> body = parseRequestBody(exchange);
            MedicalRecord r = new MedicalRecord(
                    0,
                    body.get("patientId"),
                    body.get("patientName"),
                    body.get("doctorId"),
                    body.get("doctorName"),
                    body.get("diagnosis"),
                    body.get("prescription"),
                    body.get("labTests"),
                    body.get("notes"),
                    body.getOrDefault("recordDate", LocalDate.now().toString())
            );
            recordDAO.addRecord(r);
            sendResponse(exchange, 201, JsonUtils.successJson("Medical record saved successfully."));
        } else if ("DELETE".equals(method)) {
            int id = Integer.parseInt(query.get("id"));
            recordDAO.deleteRecord(id);
            sendResponse(exchange, 200, JsonUtils.successJson("Medical record deleted."));
        }
    }

    // --- Utility Methods ---

    private void sendResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) return map;
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                map.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private Map<String, String> parseRequestBody(HttpExchange exchange) throws IOException {
        Map<String, String> map = new HashMap<>();
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();

        if (body.isEmpty()) return map;

        // Check if JSON body
        if (body.startsWith("{") && body.endsWith("}")) {
            // Simple key-value JSON string parsing for primitives
            String content = body.substring(1, body.length() - 1);
            String[] tokens = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String token : tokens) {
                String[] kv = token.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (kv.length == 2) {
                    String k = kv[0].trim().replace("\"", "");
                    String v = kv[1].trim().replace("\"", "");
                    map.put(k, v);
                }
            }
        } else {
            // Form-urlencoded parsing
            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2) {
                    map.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                }
            }
        }
        return map;
    }
}
