package com.hospital.dao;

import com.hospital.config.DatabaseConfig;
import com.hospital.exceptions.AppointmentConflictException;
import com.hospital.exceptions.DatabaseException;
import com.hospital.interfaces.AppointmentDAO;
import com.hospital.models.Appointment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of AppointmentDAO.
 * Validates doctor schedule conflicts before booking.
 */
public class AppointmentDAOImpl implements AppointmentDAO {
    private final DatabaseConfig dbConfig = DatabaseConfig.getInstance();

    @Override
    public void bookAppointment(Appointment a) throws AppointmentConflictException, DatabaseException {
        // Enforce business rule: Prevent double booking for the same doctor at the same slot
        if (!isSlotAvailable(a.getDoctorId(), a.getAppointmentDate(), a.getTimeSlot())) {
            throw new AppointmentConflictException(a.getDoctorId(), a.getAppointmentDate(), a.getTimeSlot());
        }

        String sql = "INSERT INTO appointments (patient_id, patient_name, doctor_id, doctor_name, appointment_date, time_slot, status, reason) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, a.getPatientId());
            pstmt.setString(2, a.getPatientName());
            pstmt.setString(3, a.getDoctorId());
            pstmt.setString(4, a.getDoctorName());
            pstmt.setString(5, a.getAppointmentDate());
            pstmt.setString(6, a.getTimeSlot());
            pstmt.setString(7, a.getStatus() == null ? "BOOKED" : a.getStatus());
            pstmt.setString(8, a.getReason());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error booking appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isSlotAvailable(String doctorId, String date, String timeSlot) throws DatabaseException {
        String sql = "SELECT COUNT(*) AS booked FROM appointments " +
                     "WHERE doctor_id = ? AND appointment_date = ? AND time_slot = ? AND status != 'CANCELLED'";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, doctorId);
            pstmt.setString(2, date);
            pstmt.setString(3, timeSlot);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("booked") == 0;
                }
                return true;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking slot availability: " + e.getMessage(), e);
        }
    }

    @Override
    public Appointment getAppointmentById(int id) throws DatabaseException {
        String sql = "SELECT * FROM appointments WHERE id = ?";
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
            throw new DatabaseException("Error retrieving appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Appointment> getAllAppointments() throws DatabaseException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments ORDER BY appointment_date DESC, id DESC";
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching appointments: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Appointment> getAppointmentsByDoctor(String doctorId) throws DatabaseException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments WHERE doctor_id = ? ORDER BY appointment_date ASC";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, doctorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving doctor appointments: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Appointment> getAppointmentsByPatient(String patientId) throws DatabaseException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments WHERE patient_id = ? ORDER BY appointment_date DESC";
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
            throw new DatabaseException("Error retrieving patient appointments: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateAppointmentStatus(int id, String status) throws DatabaseException {
        String sql = "UPDATE appointments SET status = ? WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error updating appointment status: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelAppointment(int id) throws DatabaseException {
        updateAppointmentStatus(id, "CANCELLED");
    }

    @Override
    public int getAppointmentCount() throws DatabaseException {
        String sql = "SELECT COUNT(*) AS total FROM appointments WHERE status = 'BOOKED'";
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("total") : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error getting appointment count: " + e.getMessage(), e);
        }
    }

    private Appointment mapRow(ResultSet rs) throws SQLException {
        return new Appointment(
                rs.getInt("id"),
                rs.getString("patient_id"),
                rs.getString("patient_name"),
                rs.getString("doctor_id"),
                rs.getString("doctor_name"),
                rs.getString("appointment_date"),
                rs.getString("time_slot"),
                rs.getString("status"),
                rs.getString("reason")
        );
    }
}
