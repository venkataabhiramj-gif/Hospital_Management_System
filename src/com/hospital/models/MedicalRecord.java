package com.hospital.models;

/**
 * MedicalRecord model capturing patient clinical consultations, diagnoses, and prescriptions.
 */
public class MedicalRecord {
    private int recordId;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String diagnosis;
    private String prescription;
    private String labTests;
    private String notes;
    private String recordDate;

    public MedicalRecord() {
    }

    public MedicalRecord(int recordId, String patientId, String patientName, String doctorId, String doctorName,
                         String diagnosis, String prescription, String labTests, String notes, String recordDate) {
        this.recordId = recordId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.labTests = labTests;
        this.notes = notes;
        this.recordDate = recordDate;
    }

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getPrescription() {
        return prescription;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
    }

    public String getLabTests() {
        return labTests;
    }

    public void setLabTests(String labTests) {
        this.labTests = labTests;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(String recordDate) {
        this.recordDate = recordDate;
    }
}
