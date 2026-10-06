package com.santsa.mediaassistant.config;

import com.santsa.mediaassistant.model.Appointment;
import com.santsa.mediaassistant.model.Doctor;
import com.santsa.mediaassistant.model.Patient;
import com.santsa.mediaassistant.repository.AppointmentRepository;
import com.santsa.mediaassistant.repository.DoctorRepository;
import com.santsa.mediaassistant.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    @Override
    public void run(String... args) {
        loadDoctors();
        loadAppointments();
        loadPatients();
        log.info("Data loaded: {} doctors, {} appointments, {} patients",
                doctorRepository.count(),
                appointmentRepository.count(),
                patientRepository.count());
    }

    private void loadDoctors() {
        doctorRepository.saveAll(List.of(
                Doctor.builder().firstName("Ana").lastName("Martinez").specialty("Cardiology")
                        .licenseNumber("MP-12345").phone("555-0101").office("Office 101").build(),
                Doctor.builder().firstName("Carlos").lastName("Lopez").specialty("Dermatology")
                        .licenseNumber("MP-23456").phone("555-0102").office("Office 205").build(),
                Doctor.builder().firstName("Laura").lastName("Gomez").specialty("Pediatrics")
                        .licenseNumber("MP-34567").phone("555-0103").office("Office 302").build(),
                Doctor.builder().firstName("Martin").lastName("Ruiz").specialty("Orthopedics")
                        .licenseNumber("MP-45678").phone("555-0104").office("Office 110").build(),
                Doctor.builder().firstName("Sofia").lastName("Chen").specialty("Endocrinology")
                        .licenseNumber("MP-56789").phone("555-0105").office("Office 408").build()));
    }

    private void loadAppointments() {
        var today = LocalDate.now();
        var doctors = doctorRepository.findAll();
        LocalTime[] times = {
                LocalTime.of(9, 0), LocalTime.of(10, 0),
                LocalTime.of(11, 0), LocalTime.of(15, 0)
        };
        int count = 0;
        for (int day = 1; day <= 5; day++) {
            for (var time : times) {
                var doctor = doctors.get(count % doctors.size());
                appointmentRepository.save(
                        Appointment.builder()
                                .doctorId(doctor.getId())
                                .date(today.plusDays(day))
                                .startTime(time)
                                .available(count % 3 != 2)
                                .build());
                count++;
            }
        }
    }

    private void loadPatients() {
        patientRepository.saveAll(List.of(
                Patient.builder().firstName("Gabriel").lastName("Example")
                        .dateOfBirth(LocalDate.of(1985, 3, 15))
                        .allergies("Penicillin")
                        .conditions("Mild hypertension controlled with medication").build(),
                Patient.builder().firstName("Maria").lastName("Test")
                        .dateOfBirth(LocalDate.of(1990, 7, 22))
                        .allergies("None known")
                        .conditions("Type 2 diabetes diagnosed in 2019").build(),
                Patient.builder().firstName("Juan").lastName("Demo")
                        .dateOfBirth(LocalDate.of(1978, 11, 8))
                        .allergies("Ibuprofen, Shellfish")
                        .conditions("No pre-existing conditions").build()));
    }

}
