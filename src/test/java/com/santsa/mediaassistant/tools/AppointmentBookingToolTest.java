package com.santsa.mediaassistant.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import com.santsa.mediaassistant.service.AppointmentService;

@ExtendWith(MockitoExtension.class)
class AppointmentBookingToolTest {

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private AppointmentBookingTool tool;

    private static ToolContext contextWithRole(String role) {
        return new ToolContext(Map.of("userId", 1L, "role", role));
    }

    @Test
    void bookAppointment_whenRoleIsAdmin_delegatesToServiceWithParsedValues() {
        when(appointmentService.bookAppointment("Cardiology", LocalDate.of(2026, 12, 1), LocalTime.of(10, 0), 5L))
                .thenReturn("Appointment booked");

        String result = tool.bookAppointment("Cardiology", "2026-12-01", "10:00", 5L, contextWithRole("ADMIN"));

        assertThat(result).isEqualTo("Appointment booked");
        verify(appointmentService).bookAppointment("Cardiology", LocalDate.of(2026, 12, 1), LocalTime.of(10, 0), 5L);
    }

    @ParameterizedTest
    @ValueSource(strings = { "PATIENT", "UNKNOWN", "admin", "" })
    void bookAppointment_whenRoleIsNotAdmin_returnsDeniedMessageAndDoesNotBook(String role) {
        String result = tool.bookAppointment("Cardiology", "2026-12-01", "10:00", 5L, contextWithRole(role));

        assertThat(result).isEqualTo("Denied access. Only admin can book appointments");
        verifyNoInteractions(appointmentService);
    }

    @Test
    void bookAppointment_whenRoleMissingFromContext_returnsDeniedMessageAndDoesNotBook() {
        ToolContext withoutRole = new ToolContext(Map.of("userId", 1L));

        String result = tool.bookAppointment("Cardiology", "2026-12-01", "10:00", 5L, withoutRole);

        assertThat(result).isEqualTo("Denied access. Only admin can book appointments");
        verifyNoInteractions(appointmentService);
    }

    @Test
    void bookAppointment_whenRoleIsNotAdmin_deniesEvenWithInvalidDate() {
        String result = tool.bookAppointment("Cardiology", "not-a-date", "10:00", 5L, contextWithRole("PATIENT"));

        assertThat(result).startsWith("Denied access");
        verifyNoInteractions(appointmentService);
    }

    @Test
    void bookAppointment_whenDateIsInvalid_throwsDateTimeParseExceptionAndDoesNotBook() {
        assertThatThrownBy(() -> tool.bookAppointment("Cardiology", "01/12/2026", "10:00", 5L, contextWithRole("ADMIN")))
                .isInstanceOf(DateTimeParseException.class);

        verifyNoInteractions(appointmentService);
    }

    @Test
    void bookAppointment_whenTimeIsInvalid_throwsDateTimeParseExceptionAndDoesNotBook() {
        assertThatThrownBy(() -> tool.bookAppointment("Cardiology", "2026-12-01", "10h", 5L, contextWithRole("ADMIN")))
                .isInstanceOf(DateTimeParseException.class);

        verifyNoInteractions(appointmentService);
    }
}
