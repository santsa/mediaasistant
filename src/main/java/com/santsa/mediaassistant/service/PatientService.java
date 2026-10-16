package com.santsa.mediaassistant.service;

import com.santsa.mediaassistant.dto.PatientInfo;

public interface PatientService {
    PatientInfo getPatientInfo(Long patientId);
}
