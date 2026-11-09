package com.hospital.web;

import com.hospital.models.*;

import java.util.List;
import java.util.Map;

/**
 * Lightweight JSON formatting utility built purely with standard Java.
 * Avoids third-party library bloat and makes the application 100% self-contained.
 */
public class JsonUtils {

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String patientToJson(Patient p) {
        return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\",\"age\":%d,\"gender\":\"%s\",\"bloodGroup\":\"%s\",\"address\":\"%s\",\"emergencyContact\":\"%s\",\"registeredDate\":\"%s\",\"role\":\"%s\"}",
                escape(p.getId()), escape(p.getName()), escape(p.getEmail()), escape(p.getPhone()), p.getAge(),
                escape(p.getGender()), escape(p.getBloodGroup()), escape(p.getAddress()),
                escape(p.getEmergencyContact()), escape(p.getRegisteredDate()), escape(p.getRole())
        );
    }

    public static String patientListToJson(List<Patient> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(patientToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String doctorToJson(Doctor d) {
        return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\",\"age\":%d,\"gender\":\"%s\",\"specialization\":\"%s\",\"qualification\":\"%s\",\"consultationFee\":%.2f,\"availableDays\":\"%s\",\"availableHours\":\"%s\",\"role\":\"%s\"}",
                escape(d.getId()), escape(d.getName()), escape(d.getEmail()), escape(d.getPhone()), d.getAge(),
                escape(d.getGender()), escape(d.getSpecialization()), escape(d.getQualification()),
                d.getConsultationFee(), escape(d.getAvailableDays()), escape(d.getAvailableHours()), escape(d.getRole())
        );
    }

    public static String doctorListToJson(List<Doctor> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(doctorToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String appointmentToJson(Appointment a) {
        return String.format(
                "{\"id\":%d,\"patientId\":\"%s\",\"patientName\":\"%s\",\"doctorId\":\"%s\",\"doctorName\":\"%s\",\"appointmentDate\":\"%s\",\"timeSlot\":\"%s\",\"status\":\"%s\",\"reason\":\"%s\"}",
                a.getId(), escape(a.getPatientId()), escape(a.getPatientName()),
                escape(a.getDoctorId()), escape(a.getDoctorName()),
                escape(a.getAppointmentDate()), escape(a.getTimeSlot()),
                escape(a.getStatus()), escape(a.getReason())
        );
    }

    public static String appointmentListToJson(List<Appointment> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(appointmentToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String billToJson(Bill b) {
        return String.format(
                "{\"billId\":%d,\"patientId\":\"%s\",\"patientName\":\"%s\",\"consultationFee\":%.2f,\"treatmentCharges\":%.2f,\"medicineCharges\":%.2f,\"discountPercent\":%.2f,\"taxPercent\":%.2f,\"totalAmount\":%.2f,\"paymentStatus\":\"%s\",\"billingDate\":\"%s\"}",
                b.getBillId(), escape(b.getPatientId()), escape(b.getPatientName()),
                b.getConsultationFee(), b.getTreatmentCharges(), b.getMedicineCharges(),
                b.getDiscountPercent(), b.getTaxPercent(), b.getTotalAmount(),
                escape(b.getPaymentStatus()), escape(b.getBillingDate())
        );
    }

    public static String billListToJson(List<Bill> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(billToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String recordToJson(MedicalRecord r) {
        return String.format(
                "{\"recordId\":%d,\"patientId\":\"%s\",\"patientName\":\"%s\",\"doctorId\":\"%s\",\"doctorName\":\"%s\",\"diagnosis\":\"%s\",\"prescription\":\"%s\",\"labTests\":\"%s\",\"notes\":\"%s\",\"recordDate\":\"%s\"}",
                r.getRecordId(), escape(r.getPatientId()), escape(r.getPatientName()),
                escape(r.getDoctorId()), escape(r.getDoctorName()),
                escape(r.getDiagnosis()), escape(r.getPrescription()),
                escape(r.getLabTests()), escape(r.getNotes()), escape(r.getRecordDate())
        );
    }

    public static String recordListToJson(List<MedicalRecord> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(recordToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String errorJson(String message) {
        return String.format("{\"success\":false,\"error\":\"%s\"}", escape(message));
    }

    public static String successJson(String message) {
        return String.format("{\"success\":true,\"message\":\"%s\"}", escape(message));
    }
}
