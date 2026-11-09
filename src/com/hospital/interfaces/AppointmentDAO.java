package com.hospital.interfaces;

import com.hospital.models.Appointment;
import com.hospital.exceptions.AppointmentConflictException;
import com.hospital.exceptions.DatabaseException;
import java.util.List;

/**
 * Data Access Object (DAO) interface for Appointment scheduling and management.
 */
public interface AppointmentDAO {
    void bookAppointment(Appointment appointment) throws AppointmentConflictException, DatabaseException;
    Appointment getAppointmentById(int id) throws DatabaseException;
    List<Appointment> getAllAppointments() throws DatabaseException;
    List<Appointment> getAppointmentsByDoctor(String doctorId) throws DatabaseException;
    List<Appointment> getAppointmentsByPatient(String patientId) throws DatabaseException;
    void updateAppointmentStatus(int id, String status) throws DatabaseException;
    void cancelAppointment(int id) throws DatabaseException;
    boolean isSlotAvailable(String doctorId, String date, String timeSlot) throws DatabaseException;
    int getAppointmentCount() throws DatabaseException;
}
