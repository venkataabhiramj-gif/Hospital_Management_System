package com.hospital.dao;

import com.hospital.config.DatabaseConfig;
import com.hospital.exceptions.DatabaseException;
import com.hospital.exceptions.InvalidBillingException;
import com.hospital.interfaces.BillingDAO;
import com.hospital.models.Bill;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of BillingDAO.
 */
public class BillingDAOImpl implements BillingDAO {
    private final DatabaseConfig dbConfig = DatabaseConfig.getInstance();

    @Override
    public void createBill(Bill bill) throws InvalidBillingException, DatabaseException {
        // Enforce calculations via interface methods before DB write
        double finalTotal = bill.calculateFinalTotal();
        bill.setTotalAmount(finalTotal);

        String sql = "INSERT INTO bills (patient_id, patient_name, consultation_fee, treatment_charges, medicine_charges, discount_percent, tax_percent, total_amount, payment_status, billing_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bill.getPatientId());
            pstmt.setString(2, bill.getPatientName());
            pstmt.setDouble(3, bill.getConsultationFee());
            pstmt.setDouble(4, bill.getTreatmentCharges());
            pstmt.setDouble(5, bill.getMedicineCharges());
            pstmt.setDouble(6, bill.getDiscountPercent());
            pstmt.setDouble(7, bill.getTaxPercent());
            pstmt.setDouble(8, bill.getTotalAmount());
            pstmt.setString(9, bill.getPaymentStatus() == null ? "PENDING" : bill.getPaymentStatus());
            pstmt.setString(10, bill.getBillingDate());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error creating bill record: " + e.getMessage(), e);
        }
    }

    @Override
    public Bill getBillById(int id) throws DatabaseException {
        String sql = "SELECT * FROM bills WHERE bill_id = ?";
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
            throw new DatabaseException("Error retrieving bill: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Bill> getAllBills() throws DatabaseException {
        List<Bill> list = new ArrayList<>();
        String sql = "SELECT * FROM bills ORDER BY bill_id DESC";
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error listing bills: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Bill> getBillsByPatient(String patientId) throws DatabaseException {
        List<Bill> list = new ArrayList<>();
        String sql = "SELECT * FROM bills WHERE patient_id = ? ORDER BY bill_id DESC";
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
            throw new DatabaseException("Error fetching bills for patient: " + e.getMessage(), e);
        }
    }

    @Override
    public void updatePaymentStatus(int billId, String status) throws DatabaseException {
        String sql = "UPDATE bills SET payment_status = ? WHERE bill_id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, billId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error updating payment status: " + e.getMessage(), e);
        }
    }

    @Override
    public double getTotalRevenue() throws DatabaseException {
        String sql = "SELECT SUM(total_amount) AS revenue FROM bills WHERE payment_status = 'PAID'";
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getDouble("revenue") : 0.0;
        } catch (SQLException e) {
            throw new DatabaseException("Error aggregating total revenue: " + e.getMessage(), e);
        }
    }

    private Bill mapRow(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setBillId(rs.getInt("bill_id"));
        b.setPatientId(rs.getString("patient_id"));
        b.setPatientName(rs.getString("patient_name"));
        b.setConsultationFee(rs.getDouble("consultation_fee"));
        b.setTreatmentCharges(rs.getDouble("treatment_charges"));
        b.setMedicineCharges(rs.getDouble("medicine_charges"));
        b.setDiscountPercent(rs.getDouble("discount_percent"));
        b.setTaxPercent(rs.getDouble("tax_percent"));
        b.setTotalAmount(rs.getDouble("total_amount"));
        b.setPaymentStatus(rs.getString("payment_status"));
        b.setBillingDate(rs.getString("billing_date"));
        return b;
    }
}
