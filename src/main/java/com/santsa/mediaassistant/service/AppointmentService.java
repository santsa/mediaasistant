package com.santsa.mediaassistant.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.santsa.mediaassistant.dto.AppointmentInfo;

public interface AppointmentService {

    List<AppointmentInfo> findAvailableAppointments(String specialty, LocalDate date);
    String bookAppointment(String speciality, LocalDate date, LocalTime time, Long patientId);

}