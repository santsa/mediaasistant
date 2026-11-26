package com.santsa.mediaassistant.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.santsa.mediaassistant.dto.AppointmentInfo;
import com.santsa.mediaassistant.service.AppointmentService;

@ExtendWith(MockitoExtension.class)
class AppointmentSearchToolTest {

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private AppointmentSearchTool tool;

    @Test
    void searchAppointments_parsesDateAndReturnsServiceResult() {
        List<AppointmentInfo> slots = List.of(new AppointmentInfo("Ana García", "cardiology", "2026-12-01", "10:00"));
        when(appointmentService.findAvailableAppointments("cardiology", LocalDate.of(2026, 12, 1))).thenReturn(slots);

        List<AppointmentInfo> result = tool.searchAppointments("cardiology", "2026-12-01");

        assertThat(result).isSameAs(slots);
    }

    @Test
    void searchAppointments_whenDateIsInvalid_throwsDateTimeParseExceptionAndDoesNotSearch() {
        assertThatThrownBy(() -> tool.searchAppointments("cardiology", "1 de diciembre"))
                .isInstanceOf(DateTimeParseException.class);

        verifyNoInteractions(appointmentService);
    }
}
