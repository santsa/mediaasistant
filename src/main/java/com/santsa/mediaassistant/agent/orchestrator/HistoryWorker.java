package com.santsa.mediaassistant.agent.orchestrator;

import org.springframework.stereotype.Component;

import com.santsa.mediaassistant.dto.PatientInfo;
import com.santsa.mediaassistant.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class HistoryWorker {
    private final PatientService patientService;

    public String lookup(Long userId) {

        PatientInfo patientInfo = patientService.getPatientInfo(userId);

        if (patientInfo == null) {
            log.info("Patient with Id = {} not found clinical history", userId);
            return "Not found clinical history for this patient.";
        }

        String result = String.format(
                "Patient; %% | Date of birth: %s | Alergies: %s | Conditions: %s",
                patientInfo.firstName(), patientInfo.lastName(),
                patientInfo.dateOfBirth(),
                patientInfo.allergies() != null ? patientInfo.allergies() : "Not registered",
                patientInfo.conditions() != null ? patientInfo.conditions() : "Not registered");

        log.info(result);
        return result;
    }
}
