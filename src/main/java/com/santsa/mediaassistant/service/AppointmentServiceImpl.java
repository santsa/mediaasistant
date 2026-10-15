package com.santsa.mediaassistant.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.santsa.mediaassistant.dto.AppointmentInfo;
import com.santsa.mediaassistant.model.Appointment;
import com.santsa.mediaassistant.model.Doctor;
import com.santsa.mediaassistant.repository.AppointmentRepository;
import com.santsa.mediaassistant.repository.DoctorRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public List<AppointmentInfo> findAvailableAppointments(String specialty, LocalDate date) {
        log.info("Fetching appointments for specialty: {} on date: {}", specialty, date);

        var doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        if (doctors.isEmpty()) {
            log.warn("No doctors found for specialty: {}", specialty);
            return List.of();
        }
        var doctorNames = buildDoctorNameMap(doctors);
        var doctorsIds = new ArrayList<>(doctorNames.keySet());

        var appointments = appointmentRepository.findByDoctorIdInAndDateAndAvailableTrue(doctorsIds, date);
        return toAppointmentsInfoList(appointments, doctorNames, specialty);
    }

    @SuppressWarnings("null")
    private Map<Long, String> buildDoctorNameMap(List<Doctor> doctors) {
        return doctors.stream()
                .collect(Collectors.toMap(
                        Doctor::getId,
                        doctor -> doctor.getFirstName() + " " + doctor.getLastName()));
    }

    private List<AppointmentInfo> toAppointmentsInfoList(
            List<Appointment> appointments,
            Map<Long, String> doctorNames,
            String specialty) {
        return appointments.stream()
                .map(appointment -> new AppointmentInfo(
                        doctorNames.get(appointment.getDoctorId()),
                        specialty,
                        appointment.getDate().toString(),
                        appointment.getStartTime().toString()))
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public String bookAppointment(String speciality, LocalDate date, LocalTime time, Long patientId) {
        log.info("bookAppointment appointments for specialty: {} on date: {}, on time: {}, patientId: {}", speciality,
                date, time, patientId);

        var doctors = doctorRepository.findBySpecialtyIgnoreCase(speciality);
        if (doctors.isEmpty()) {
            return "Not found speciality: " + speciality;
        }

        var doctorIds = doctors.stream().map(Doctor::getId).toList();
        var appointment = findAvailableAppointment(doctorIds, date, time);
        if (appointment == null) {
            return "Not found turn";
        }

        appointment.setAvailable(false);
        appointment.setPatientId(patientId);
        appointmentRepository.save(appointment);

        return "Reserved turn succesfully";
    }

    private Appointment findAvailableAppointment(List<Long> doctorIds, LocalDate date, LocalTime startTime) {
        return appointmentRepository.findByDoctorIdInAndDateAndStartTimeAndAvailableTrue(doctorIds, date, startTime)
                .stream()
                .findFirst()
                .orElse(null);
    }
}
