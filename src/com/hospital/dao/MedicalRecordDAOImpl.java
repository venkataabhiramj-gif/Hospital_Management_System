package com.hospital.dao;

import com.hospital.config.DatabaseConfig;
import com.hospital.exceptions.DatabaseException;
import com.hospital.interfaces.MedicalRecordDAO;
import com.hospital.models.MedicalRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of MedicalRecordDAO.
 */
public class MedicalRecordDAOImpl implements MedicalRecordDAO {
    private final DatabaseConfig dbConfig = DatabaseConfig.getInstance();

    @Override
    public void addRecord(MedicalRecord r) throws DatabaseException {
        String sql = "INSERT INTO medical_records (patient_id, patient_name, doctor_id, doctor_name, diagnosis, prescription, lab_tests, notes, record_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, r.getPatientId());
            pstmt.setString(2, r.getPatientName());
            pstmt.setString(3, r.getDoctorId());
            pstmt.setString(4, r.getDoctorName());
            pstmt.setString(5, r.getDiagnosis());
            pstmt.setString(6, r.getPrescription());
            pstmt.setString(7, r.getLabTests());
            pstmt.setString(8, r.getNotes());
            pstmt.setString(9, r.getRecordDate());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error creating medical record: " + e.getMessage(), e);
        }
    }

    @Override
    public MedicalRecord getRecordById(int id) throws DatabaseException {
        String sql = "SELECT * FROM medical_records WHERE record_id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving record: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MedicalRecord> getRecordsByPatient(String patientId) throws DatabaseException {
        List<MedicalRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM medical_records WHERE patient_id = ? ORDER BY record_date DESC, record_id DESC";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching records for patient: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MedicalRecord> getAllRecords() throws DatabaseException {
        List<MedicalRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM medical_records ORDER BY record_date DESC, record_id DESC";
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error listing medical records: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteRecord(int id) throws DatabaseException {
        String sql = "DELETE FROM medical_records WHERE record_id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting medical record: " + e.getMessage(), e);
        }
    }

    private MedicalRecord mapRow(ResultSet rs) throws SQLException {
        return new MedicalRecord(
                rs.getInt("record_id"),
                rs.getString("patient_id"),
                rs.getString("patient_name"),
                rs.getString("doctor_id"),
                rs.getString("doctor_name"),
                rs.getString("diagnosis"),
                rs.getString("prescription"),
                rs.getString("lab_tests"),
                rs.getString("notes"),
                rs.getString("record_date")
        );
    }
}
