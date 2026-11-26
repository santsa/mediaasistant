package com.santsa.mediaassistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.santsa.mediaassistant.dto.PatientInfo;
import com.santsa.mediaassistant.model.Patient;
import com.santsa.mediaassistant.repository.PatientRepository;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientServiceImpl service;

    @Test
    void getPatientInfo_whenPatientExists_mapsAllFieldsToPatientInfo() {
        Patient patient = Patient.builder().id(1L).firstName("Juan").lastName("López")
                .dateOfBirth(LocalDate.of(1980, 5, 17)).allergies("Penicillin").conditions("Hypertension").build();
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        PatientInfo result = service.getPatientInfo(1L);

        assertThat(result).isEqualTo(new PatientInfo("Juan", "López", "1980-05-17", "Penicillin", "Hypertension"));
    }

    @Test
    void getPatientInfo_whenDateOfBirthIsNull_mapsItAsNull() {
        Patient patient = Patient.builder().id(1L).firstName("Juan").lastName("López").build();
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        assertThat(service.getPatientInfo(1L)).isEqualTo(new PatientInfo("Juan", "López", null, null, null));
    }

    @Test
    void getPatientInfo_whenPatientDoesNotExist_returnsNull() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.getPatientInfo(99L)).isNull();
    }
}
