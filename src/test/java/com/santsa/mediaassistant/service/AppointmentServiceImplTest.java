package com.santsa.mediaassistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.santsa.mediaassistant.dto.AppointmentInfo;
import com.santsa.mediaassistant.model.Appointment;
import com.santsa.mediaassistant.model.Doctor;
import com.santsa.mediaassistant.repository.AppointmentRepository;
import com.santsa.mediaassistant.repository.DoctorRepository;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 12, 1);
    private static final LocalTime TIME = LocalTime.of(10, 0);

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentServiceImpl service;

    private static Doctor doctor(Long id, String firstName, String lastName) {
        return Doctor.builder().id(id).firstName(firstName).lastName(lastName).specialty("Cardiology").build();
    }

    private static Appointment slot(Long id, Long doctorId, LocalTime time) {
        return Appointment.builder().id(id).doctorId(doctorId).date(DATE).startTime(time).available(true).build();
    }

    // --- findAvailableAppointments ---

    @Test
    void findAvailableAppointments_whenNoDoctorsForSpecialty_returnsEmptyListWithoutQueryingAppointments() {
        when(doctorRepository.findBySpecialtyIgnoreCase("Dermatology")).thenReturn(List.of());

        List<AppointmentInfo> result = service.findAvailableAppointments("Dermatology", DATE);

        assertThat(result).isEmpty();
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void findAvailableAppointments_whenSlotsExist_mapsThemWithDoctorFullName() {
        when(doctorRepository.findBySpecialtyIgnoreCase("cardiology"))
                .thenReturn(List.of(doctor(1L, "Ana", "García"), doctor(2L, "Luis", "Pérez")));
        when(appointmentRepository.findByDoctorIdInAndDateAndAvailableTrue(any(), any()))
                .thenReturn(List.of(slot(10L, 1L, TIME), slot(11L, 2L, LocalTime.of(11, 30))));

        List<AppointmentInfo> result = service.findAvailableAppointments("cardiology", DATE);

        assertThat(result).containsExactly(
                new AppointmentInfo("Ana García", "cardiology", "2026-12-01", "10:00"),
                new AppointmentInfo("Luis Pérez", "cardiology", "2026-12-01", "11:30"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAvailableAppointments_queriesAppointmentsForAllDoctorIdsAndDate() {
        when(doctorRepository.findBySpecialtyIgnoreCase("cardiology"))
                .thenReturn(List.of(doctor(1L, "Ana", "García"), doctor(2L, "Luis", "Pérez")));
        when(appointmentRepository.findByDoctorIdInAndDateAndAvailableTrue(any(), any())).thenReturn(List.of());
        ArgumentCaptor<List<Long>> ids = ArgumentCaptor.forClass(List.class);

        List<AppointmentInfo> result = service.findAvailableAppointments("cardiology", DATE);

        assertThat(result).isEmpty();
        verify(appointmentRepository).findByDoctorIdInAndDateAndAvailableTrue(ids.capture(), any());
        assertThat(ids.getValue()).containsExactlyInAnyOrder(1L, 2L);
    }

    // --- bookAppointment ---

    @Test
    void bookAppointment_whenNoDoctorsForSpecialty_returnsNotFoundSpecialityAndDoesNotSave() {
        when(doctorRepository.findBySpecialtyIgnoreCase("Dermatology")).thenReturn(List.of());

        String result = service.bookAppointment("Dermatology", DATE, TIME, 5L);

        assertThat(result).isEqualTo("Not found speciality: Dermatology");
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void bookAppointment_whenNoFreeSlotAtThatTime_returnsNotFoundTurnAndDoesNotSave() {
        when(doctorRepository.findBySpecialtyIgnoreCase("Cardiology")).thenReturn(List.of(doctor(1L, "Ana", "García")));
        when(appointmentRepository.findByDoctorIdInAndDateAndStartTimeAndAvailableTrue(List.of(1L), DATE, TIME))
                .thenReturn(List.of());

        String result = service.bookAppointment("Cardiology", DATE, TIME, 5L);

        assertThat(result).isEqualTo("Not found turn");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookAppointment_whenFreeSlotExists_marksFirstSlotAsTakenByPatientAndSaves() {
        Appointment first = slot(10L, 1L, TIME);
        Appointment second = slot(11L, 2L, TIME);
        when(doctorRepository.findBySpecialtyIgnoreCase("Cardiology"))
                .thenReturn(List.of(doctor(1L, "Ana", "García"), doctor(2L, "Luis", "Pérez")));
        when(appointmentRepository.findByDoctorIdInAndDateAndStartTimeAndAvailableTrue(List.of(1L, 2L), DATE, TIME))
                .thenReturn(List.of(first, second));

        String result = service.bookAppointment("Cardiology", DATE, TIME, 5L);

        assertThat(result).isEqualTo("Reserved turn succesfully");
        assertThat(first.isAvailable()).isFalse();
        assertThat(first.getPatientId()).isEqualTo(5L);
        assertThat(second.isAvailable()).isTrue();
        verify(appointmentRepository).save(first);
    }
}
