package com.hospital.interfaces;

import com.hospital.models.Patient;
import com.hospital.exceptions.DatabaseException;
import com.hospital.exceptions.PatientNotFoundException;
import java.util.List;

/**
 * Data Access Object (DAO) interface for Patient persistence.
 * Academic Concept: Interface Abstraction & Decoupling Data Access from Business Logic.
 */
public interface PatientDAO {
    void addPatient(Patient patient) throws DatabaseException;
    Patient getPatientById(String id) throws PatientNotFoundException, DatabaseException;
    List<Patient> getAllPatients() throws DatabaseException;
    List<Patient> searchPatients(String keyword) throws DatabaseException;
    void updatePatient(Patient patient) throws PatientNotFoundException, DatabaseException;
    void deletePatient(String id) throws PatientNotFoundException, DatabaseException;
    int getPatientCount() throws DatabaseException;
}
