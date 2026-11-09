package com.hospital.exceptions;

/**
 * Thrown when an appointment cannot be scheduled because the requested doctor
 * is already booked for that specific date and time slot.
 */
public class AppointmentConflictException extends HospitalException {
    private final String doctorId;
    private final String date;
    private final String timeSlot;

    public AppointmentConflictException(String doctorId, String date, String timeSlot) {
        super(String.format("Doctor %s is already booked on %s during slot %s.", doctorId, date, timeSlot));
        this.doctorId = doctorId;
        this.date = date;
        this.timeSlot = timeSlot;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getDate() {
        return date;
    }

    public String getTimeSlot() {
        return timeSlot;
    }
}
