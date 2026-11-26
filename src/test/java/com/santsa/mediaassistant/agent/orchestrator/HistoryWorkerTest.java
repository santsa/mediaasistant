package com.santsa.mediaassistant.agent.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.santsa.mediaassistant.dto.PatientInfo;
import com.santsa.mediaassistant.service.PatientService;

@ExtendWith(MockitoExtension.class)
class HistoryWorkerTest {

    @Mock
    private PatientService patientService;

    @InjectMocks
    private HistoryWorker worker;

    @Test
    void lookup_whenPatientNotFound_returnsNotFoundMessage() {
        when(patientService.getPatientInfo(99L)).thenReturn(null);

        assertThat(worker.lookup(99L)).isEqualTo("Not found clinical history for this patient.");
    }

    @Test
    void lookup_whenPatientFound_formatsEachFieldInItsPlace() {
        when(patientService.getPatientInfo(1L))
                .thenReturn(new PatientInfo("Juan", "López", "1980-05-17", "Penicillin", "Hypertension"));

        assertThat(worker.lookup(1L)).isEqualTo(
                "Patient: Juan López | Date of birth: 1980-05-17 | Alergies: Penicillin | Conditions: Hypertension");
    }

    @Test
    void lookup_whenOptionalFieldsNull_printsNotRegistered() {
        when(patientService.getPatientInfo(1L)).thenReturn(new PatientInfo("Juan", "López", null, null, null));

        assertThat(worker.lookup(1L)).isEqualTo(
                "Patient: Juan López | Date of birth: Not registered | Alergies: Not registered | Conditions: Not registered");
    }
}
