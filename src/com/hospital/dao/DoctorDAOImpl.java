package com.hospital.dao;

import com.hospital.config.DatabaseConfig;
import com.hospital.exceptions.DatabaseException;
import com.hospital.exceptions.DoctorNotFoundException;
import com.hospital.interfaces.DoctorDAO;
import com.hospital.models.Doctor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of DoctorDAO using JDBC PreparedStatement.
 */
public class DoctorDAOImpl implements DoctorDAO {
    private final DatabaseConfig dbConfig = DatabaseConfig.getInstance();

    @Override
    public void addDoctor(Doctor d) throws DatabaseException {
        String sql = "INSERT INTO doctors (id, name, email, phone, age, gender, specialization, qualification, consultation_fee, available_days, available_hours) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, d.getId());
            pstmt.setString(2, d.getName());
            pstmt.setString(3, d.getEmail());
            pstmt.setString(4, d.getPhone());
            pstmt.setInt(5, d.getAge());
            pstmt.setString(6, d.getGender());
            pstmt.setString(7, d.getSpecialization());
            pstmt.setString(8, d.getQualification());
            pstmt.setDouble(9, d.getConsultationFee());
            pstmt.setString(10, d.getAvailableDays());
            pstmt.setString(11, d.getAvailableHours());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting doctor: " + e.getMessage(), e);
        }
    }

    @Override
    public Doctor getDoctorById(String id) throws DoctorNotFoundException, DatabaseException {
        String sql = "SELECT * FROM doctors WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                } else {
                    throw new DoctorNotFoundException(id);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving doctor: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Doctor> getAllDoctors() throws DatabaseException {
        List<Doctor> list = new ArrayList<>();
        String sql = "SELECT * FROM doctors ORDER BY name ASC";
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error listing doctors: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Doctor> getDoctorsBySpecialization(String specialization) throws DatabaseException {
        List<Doctor> list = new ArrayList<>();
        String sql = "SELECT * FROM doctors WHERE specialization LIKE ? ORDER BY name ASC";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + specialization + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error filtering doctors by specialization: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateDoctor(Doctor d) throws DoctorNotFoundException, DatabaseException {
        String sql = "UPDATE doctors SET name=?, email=?, phone=?, age=?, gender=?, specialization=?, qualification=?, consultation_fee=?, available_days=?, available_hours=? WHERE id=?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, d.getName());
            pstmt.setString(2, d.getEmail());
            pstmt.setString(3, d.getPhone());
            pstmt.setInt(4, d.getAge());
            pstmt.setString(5, d.getGender());
            pstmt.setString(6, d.getSpecialization());
            pstmt.setString(7, d.getQualification());
            pstmt.setDouble(8, d.getConsultationFee());
            pstmt.setString(9, d.getAvailableDays());
            pstmt.setString(10, d.getAvailableHours());
            pstmt.setString(11, d.getId());
            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new DoctorNotFoundException(d.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating doctor profile: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteDoctor(String id) throws DoctorNotFoundException, DatabaseException {
        String sql = "DELETE FROM doctors WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new DoctorNotFoundException(id);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting doctor record: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDoctorCount() throws DatabaseException {
        String sql = "SELECT COUNT(*) AS total FROM doctors";
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("total") : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error getting doctor count: " + e.getMessage(), e);
        }
    }

    private Doctor mapRow(ResultSet rs) throws SQLException {
        return new Doctor(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getInt("age"),
                rs.getString("gender"),
                rs.getString("specialization"),
                rs.getString("qualification"),
                rs.getDouble("consultation_fee"),
                rs.getString("available_days"),
                rs.getString("available_hours")
        );
    }
}
