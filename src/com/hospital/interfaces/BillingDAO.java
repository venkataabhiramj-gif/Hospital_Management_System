package com.hospital.interfaces;

import com.hospital.models.Bill;
import com.hospital.exceptions.DatabaseException;
import com.hospital.exceptions.InvalidBillingException;
import java.util.List;

/**
 * Data Access Object (DAO) interface for Billing and Invoicing.
 */
public interface BillingDAO {
    void createBill(Bill bill) throws InvalidBillingException, DatabaseException;
    Bill getBillById(int id) throws DatabaseException;
    List<Bill> getAllBills() throws DatabaseException;
    List<Bill> getBillsByPatient(String patientId) throws DatabaseException;
    void updatePaymentStatus(int billId, String status) throws DatabaseException;
    double getTotalRevenue() throws DatabaseException;
}
