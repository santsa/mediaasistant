package com.santsa.mediaassistant.service;

import org.springframework.stereotype.Service;

import com.santsa.mediaassistant.dto.PatientInfo;
import com.santsa.mediaassistant.model.Patient;
import com.santsa.mediaassistant.repository.PatientRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    @Override
    public PatientInfo getPatientInfo(Long patientId) {
        log.info("Fetching patient info for patientId: {}", patientId);
        return patientRepository.findById(patientId)
                .map(this::toPatientInfo)
                .orElse(null);
    }

    private PatientInfo toPatientInfo(Patient patient) {
        return new PatientInfo(
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth().toString(),
                patient.getAllergies(),
                patient.getConditions());
    }

}
