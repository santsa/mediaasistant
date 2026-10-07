package com.santsa.mediaassistant.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.santsa.mediaassistant.dto.DoctorInfo;
import com.santsa.mediaassistant.model.Doctor;
import com.santsa.mediaassistant.repository.DoctorRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j 
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public List<DoctorInfo> searchDoctors(String query) {
        log.info("Searching doctors with query: {}", query);
        return doctorRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(query, query, query)
                .stream()
                .map(this::toDoctorInfo)
                .toList();
    }

    private DoctorInfo toDoctorInfo(Doctor doctor) {
        return new DoctorInfo(
                doctor.getFirstName(),
                doctor.getLastName(),
                doctor.getSpecialty(),
                doctor.getLicenseNumber(),
                doctor.getPhone(),
                doctor.getOffice());
    }

}
