package com.hospital.interfaces;

import com.hospital.models.MedicalRecord;
import com.hospital.exceptions.DatabaseException;
import java.util.List;

/**
 * Data Access Object (DAO) interface for Medical Records & Clinical Consultations.
 */
public interface MedicalRecordDAO {
    void addRecord(MedicalRecord record) throws DatabaseException;
    MedicalRecord getRecordById(int id) throws DatabaseException;
    List<MedicalRecord> getRecordsByPatient(String patientId) throws DatabaseException;
    List<MedicalRecord> getAllRecords() throws DatabaseException;
    void deleteRecord(int id) throws DatabaseException;
}
