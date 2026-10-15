package com.santsa.mediaassistant.repository;

import com.santsa.mediaassistant.model.Appointment;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdInAndDateAndAvailableTrue(List<Long> doctorIds, LocalDate date);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Appointment> findByDoctorIdInAndDateAndStartTimeAndAvailableTrue(List<Long> doctorIds, LocalDate date, LocalTime startTime);
}
