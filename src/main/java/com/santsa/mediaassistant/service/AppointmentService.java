package com.santsa.mediaassistant.service;

import java.time.LocalDate;
import java.util.List;

import com.santsa.mediaassistant.dto.AppointmentInfo;

public interface AppointmentService {

    List<AppointmentInfo> findAvailableAppointments(String specialty, LocalDate date);

}