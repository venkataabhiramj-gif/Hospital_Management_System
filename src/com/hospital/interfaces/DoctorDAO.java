package com.hospital.interfaces;

import com.hospital.models.Doctor;
import com.hospital.exceptions.DatabaseException;
import com.hospital.exceptions.DoctorNotFoundException;
import java.util.List;

/**
 * Data Access Object (DAO) interface for Doctor persistence.
 */
public interface DoctorDAO {
    void addDoctor(Doctor doctor) throws DatabaseException;
    Doctor getDoctorById(String id) throws DoctorNotFoundException, DatabaseException;
    List<Doctor> getAllDoctors() throws DatabaseException;
    List<Doctor> getDoctorsBySpecialization(String specialization) throws DatabaseException;
    void updateDoctor(Doctor doctor) throws DoctorNotFoundException, DatabaseException;
    void deleteDoctor(String id) throws DoctorNotFoundException, DatabaseException;
    int getDoctorCount() throws DatabaseException;
}
