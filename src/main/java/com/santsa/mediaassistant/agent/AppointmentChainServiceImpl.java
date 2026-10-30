package com.santsa.mediaassistant.agent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.dto.AppointmentInfo;
import com.santsa.mediaassistant.dto.AppointmentSelection;
import com.santsa.mediaassistant.dto.agent.AppointmentRequest;
import com.santsa.mediaassistant.service.AppointmentService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentChainServiceImpl implements AppointmentChainService {

    private final ClientResolver clientResolver;
    private final AppointmentService appointmentService;
    private final PendingBookingStore pendingBookingStore;

    @Value("classpath:prompts/appointment-extraction.st")
    private Resource extractionResource;

    @Value("classpath:prompts/appointment-confirmation.st")
    private Resource confirmationResource;

    private PromptTemplate extractionTemplate;
    private PromptTemplate confirmationTemplate;

    @PostConstruct
    void init() {
        extractionTemplate = new PromptTemplate(extractionResource);
        confirmationTemplate = new PromptTemplate(confirmationResource);
    }

    @Override
    public String bookAppointmentChain(String userRequest, String model, Long UserId) {

        AppointmentRequest extracted = interpret(userRequest, model);
        List<AppointmentInfo> available = search(extracted);

        return select(userRequest, available, model, UserId);
    }

    private AppointmentRequest interpret(String userRequest, String model) {

        String prompt = extractionTemplate.render(Map.of("order", userRequest));

        AppointmentRequest extracted = ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("You are a data extractor. " +
                        "You must to extract exactly the text contain, without change and interpret nothing")
                .user(prompt)
                .call()
                .entity(AppointmentRequest.class);

        log.info("Chain step 1 - extracted: speciallity={}, date={}", extracted.specialty(), extracted.date());
        return extracted;
    }

    private List<AppointmentInfo> search(AppointmentRequest extracted) {

        List<AppointmentInfo> available = appointmentService
                .findAvailableAppointments(extracted.specialty(), LocalDate.parse(extracted.date()));

        log.info("Chain step 2 - found turn: {}", available.size());
        return available;
    }

    private String select(String userRequest, List<AppointmentInfo> available,
            String model, Long userId) {

        if (available.isEmpty()) {
            return "not found available appointments for this speciality and data. Should you try with other data";
        }
        AppointmentSelection selection = selectAppointment(userRequest, available, model);

        pendingBookingStore.store(userId, selection);

        return selection.message();
    }

    private AppointmentSelection selectAppointment(String userRequest, List<AppointmentInfo> available, String model) {
        String prompt = confirmationTemplate.render(Map.of(
                "order", userRequest,
                "appointments", available.toString()));

        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("You are a gestion assitant of medical appointments. " +
                        "Select the best appointment and complete all fields")
                .user(prompt)
                .call()
                .entity(AppointmentSelection.class);
    }

    @Override
    public String confirmBooking(Long userId) {

        AppointmentSelection selection = pendingBookingStore.remove(userId);

        if (selection == null) {
            return "You don't have any appointment pending to confirm";
        }

        if (!isStillAvailable(selection)) {
            log.info("Not available apointment to userid={}: {} {} {}",
                    userId, selection.specialty(), selection.date(), selection.time());
            return "Selected appointment aren't available. Could you search other.";
        }

        return book(selection, userId);
    }

    private String book(AppointmentSelection selection, Long userId) {

        String result = appointmentService.bookAppointment(
                selection.specialty(),
                LocalDate.parse(selection.date()),
                LocalTime.parse(selection.time()),
                userId);

        log.info("Result to the booking: {}", result);
        return result;
    }

    private boolean isStillAvailable(AppointmentSelection selection) {
        List<AppointmentInfo> available = appointmentService.findAvailableAppointments(
                selection.specialty(), LocalDate.parse(selection.date()));

        return available.stream().anyMatch(a -> a.time().equals(selection.time()));
    }

    @Override
    public String cancelBooking(Long userId) {
        AppointmentSelection selection = pendingBookingStore.remove(userId);

        if (selection == null) {
            return "You don't have any appointment pending to cancel";
        }

        log.info("Cancelapointment to userid={}: {} {} {}",
                userId, selection.specialty(), selection.date(), selection.time());
        return "Appointment canceled. Could you search other.";
    }

}
