package com.hospital.dao;

import com.hospital.config.DatabaseConfig;
import com.hospital.exceptions.DatabaseException;
import com.hospital.exceptions.PatientNotFoundException;
import com.hospital.interfaces.PatientDAO;
import com.hospital.models.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of PatientDAO using JDBC PreparedStatement.
 * Academic Concept: Secure Parameterized Queries & Resource Management (try-with-resources).
 */
public class PatientDAOImpl implements PatientDAO {
    private final DatabaseConfig dbConfig = DatabaseConfig.getInstance();

    @Override
    public void addPatient(Patient p) throws DatabaseException {
        String sql = "INSERT INTO patients (id, name, email, phone, age, gender, blood_group, address, emergency_contact, registered_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getId());
            pstmt.setString(2, p.getName());
            pstmt.setString(3, p.getEmail());
            pstmt.setString(4, p.getPhone());
            pstmt.setInt(5, p.getAge());
            pstmt.setString(6, p.getGender());
            pstmt.setString(7, p.getBloodGroup());
            pstmt.setString(8, p.getAddress());
            pstmt.setString(9, p.getEmergencyContact());
            pstmt.setString(10, p.getRegisteredDate());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting patient: " + e.getMessage(), e);
        }
    }

    @Override
    public Patient getPatientById(String id) throws PatientNotFoundException, DatabaseException {
        String sql = "SELECT * FROM patients WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                } else {
                    throw new PatientNotFoundException(id);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving patient by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Patient> getAllPatients() throws DatabaseException {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT * FROM patients ORDER BY registered_date DESC, id DESC";
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching patient directory: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Patient> searchPatients(String keyword) throws DatabaseException {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT * FROM patients WHERE id LIKE ? OR name LIKE ? OR phone LIKE ? ORDER BY id DESC";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String queryParam = "%" + keyword + "%";
            pstmt.setString(1, queryParam);
            pstmt.setString(2, queryParam);
            pstmt.setString(3, queryParam);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error searching patients: " + e.getMessage(), e);
        }
    }

    @Override
    public void updatePatient(Patient p) throws PatientNotFoundException, DatabaseException {
        String sql = "UPDATE patients SET name=?, email=?, phone=?, age=?, gender=?, blood_group=?, address=?, emergency_contact=? WHERE id=?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getName());
            pstmt.setString(2, p.getEmail());
            pstmt.setString(3, p.getPhone());
            pstmt.setInt(4, p.getAge());
            pstmt.setString(5, p.getGender());
            pstmt.setString(6, p.getBloodGroup());
            pstmt.setString(7, p.getAddress());
            pstmt.setString(8, p.getEmergencyContact());
            pstmt.setString(9, p.getId());
            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new PatientNotFoundException(p.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating patient profile: " + e.getMessage(), e);
        }
    }

    @Override
    public void deletePatient(String id) throws PatientNotFoundException, DatabaseException {
        String sql = "DELETE FROM patients WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new PatientNotFoundException(id);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting patient record: " + e.getMessage(), e);
        }
    }

    @Override
    public int getPatientCount() throws DatabaseException {
        String sql = "SELECT COUNT(*) AS total FROM patients";
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("total") : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error getting patient count: " + e.getMessage(), e);
        }
    }

    private Patient mapRow(ResultSet rs) throws SQLException {
        return new Patient(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getInt("age"),
                rs.getString("gender"),
                rs.getString("blood_group"),
                rs.getString("address"),
                rs.getString("emergency_contact"),
                rs.getString("registered_date")
        );
    }
}
