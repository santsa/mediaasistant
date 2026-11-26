package com.santsa.mediaassistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.santsa.mediaassistant.dto.DoctorInfo;
import com.santsa.mediaassistant.model.Doctor;
import com.santsa.mediaassistant.repository.DoctorRepository;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImpTest {

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorServiceImp service;

    @Test
    void searchDoctors_usesSameQueryForNameLastNameAndSpecialty() {
        when(doctorRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
                        "card", "card", "card"))
                .thenReturn(List.of());

        service.searchDoctors("card");

        verify(doctorRepository)
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
                        "card", "card", "card");
    }

    @Test
    void searchDoctors_whenDoctorsFound_mapsAllFieldsToDoctorInfo() {
        Doctor doctor = Doctor.builder().id(1L).firstName("Ana").lastName("García").specialty("Cardiology")
                .licenseNumber("LIC-1").phone("600000000").office("A-12").build();
        when(doctorRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
                        "García", "García", "García"))
                .thenReturn(List.of(doctor));

        List<DoctorInfo> result = service.searchDoctors("García");

        assertThat(result).containsExactly(
                new DoctorInfo("Ana", "García", "Cardiology", "LIC-1", "600000000", "A-12"));
    }

    @Test
    void searchDoctors_whenNothingFound_returnsEmptyList() {
        when(doctorRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
                        "xyz", "xyz", "xyz"))
                .thenReturn(List.of());

        assertThat(service.searchDoctors("xyz")).isEmpty();
    }
}
