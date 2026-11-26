package com.santsa.mediaassistant.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.dto.AppointmentInfo;
import com.santsa.mediaassistant.dto.AppointmentSelection;
import com.santsa.mediaassistant.service.AppointmentService;
import com.santsa.mediaassistant.support.FakeChatModel;

@ExtendWith(MockitoExtension.class)
class AppointmentChainServiceImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 12, 1);
    private static final AppointmentInfo SLOT_10 = new AppointmentInfo("Ana García", "cardiology", "2026-12-01", "10:00");
    private static final AppointmentSelection SELECTION =
            new AppointmentSelection("cardiology", "2026-12-01", "10:00", "Ana García", "Found you a slot at 10:00");

    @Mock
    private ClientResolver clientResolver;

    @Mock
    private AppointmentService appointmentService;

    private PendingBookingStore store;
    private AppointmentChainServiceImpl service;

    @BeforeEach
    void setUp() {
        store = new PendingBookingStore();
        service = new AppointmentChainServiceImpl(clientResolver, appointmentService, store);
        ReflectionTestUtils.setField(service, "extractionResource", new ClassPathResource("prompts/appointment-extraction.st"));
        ReflectionTestUtils.setField(service, "confirmationResource", new ClassPathResource("prompts/appointment-confirmation.st"));
        service.init();
    }

    @Nested
    class BookAppointmentChain {

        private FakeChatModel model;

        @BeforeEach
        void setUpModel() {
            model = new FakeChatModel(
                    "{\"specialty\":\"cardiology\",\"date\":\"2026-12-01\"}",
                    """
                    {"specialty":"cardiology","date":"2026-12-01","time":"10:00",
                     "doctorName":"Ana García","message":"Found you a slot at 10:00"}
                    """);
            when(clientResolver.resolveModel("gemini")).thenReturn(model);
        }

        @Test
        void whenSlotsAvailable_storesSelectionAndReturnsItsMessage() {
            when(appointmentService.findAvailableAppointments("cardiology", DATE)).thenReturn(List.of(SLOT_10));

            String result = service.bookAppointmentChain("Cardiologist on December 1st please", "gemini", 1L);

            assertThat(result).isEqualTo("Found you a slot at 10:00");
            assertThat(store.retrieve(1L)).isEqualTo(SELECTION);
        }

        @Test
        void sendsUserRequestToExtractionAndAvailableSlotsToSelection() {
            when(appointmentService.findAvailableAppointments("cardiology", DATE)).thenReturn(List.of(SLOT_10));

            service.bookAppointmentChain("Cardiologist on December 1st please", "gemini", 1L);

            assertThat(model.callCount()).isEqualTo(2);
            assertThat(model.prompts().get(0).getUserMessage().getText())
                    .contains("Cardiologist on December 1st please");
            assertThat(model.lastUserText()).contains("Cardiologist on December 1st please").contains("Ana García");
        }

        @Test
        void whenNoSlotsAvailable_returnsNotFoundWithoutSelectionCallOrStoring() {
            when(appointmentService.findAvailableAppointments("cardiology", DATE)).thenReturn(List.of());

            String result = service.bookAppointmentChain("Cardiologist on December 1st", "gemini", 1L);

            assertThat(result).startsWith("not found available appointments");
            assertThat(model.callCount()).isEqualTo(1);
            assertThat(store.retrieve(1L)).isNull();
        }
    }

    @Nested
    class ConfirmBooking {

        @Test
        void whenNothingPending_returnsMessageAndDoesNotTouchService() {
            String result = service.confirmBooking(1L);

            assertThat(result).isEqualTo("You don't have any appointment pending to confirm");
            verifyNoInteractions(appointmentService);
        }

        @Test
        void whenSlotStillAvailable_booksItForTheUserAndClearsPending() {
            store.store(1L, SELECTION);
            when(appointmentService.findAvailableAppointments("cardiology", DATE)).thenReturn(List.of(SLOT_10));
            when(appointmentService.bookAppointment("cardiology", DATE, LocalTime.of(10, 0), 1L))
                    .thenReturn("Reserved turn succesfully");

            String result = service.confirmBooking(1L);

            assertThat(result).isEqualTo("Reserved turn succesfully");
            assertThat(store.retrieve(1L)).isNull();
        }

        @Test
        void whenSlotWasTakenMeanwhile_doesNotBookAndClearsPending() {
            store.store(1L, SELECTION);
            when(appointmentService.findAvailableAppointments("cardiology", DATE))
                    .thenReturn(List.of(new AppointmentInfo("Ana García", "cardiology", "2026-12-01", "11:00")));

            String result = service.confirmBooking(1L);

            assertThat(result).isEqualTo("Selected appointment aren't available. Could you search other.");
            verify(appointmentService, never()).bookAppointment(anyString(), any(), any(), anyLong());
            assertThat(store.retrieve(1L)).isNull();
        }
    }

    @Nested
    class CancelBooking {

        @Test
        void whenPending_removesItAndConfirmsCancellation() {
            store.store(1L, SELECTION);

            String result = service.cancelBooking(1L);

            assertThat(result).isEqualTo("Appointment canceled. Could you search other.");
            assertThat(store.retrieve(1L)).isNull();
            verifyNoInteractions(appointmentService);
        }

        @Test
        void whenNothingPending_returnsMessage() {
            assertThat(service.cancelBooking(1L)).isEqualTo("You don't have any appointment pending to cancel");
        }
    }
}
