package com.santsa.mediaassistant.agent;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.dto.AppointmentInfo;
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

        return confirm(userRequest, available, model);
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

    private String confirm(String userRequest, List<AppointmentInfo> available, String model) {
        String prompt = confirmationTemplate.render(Map.of(
                "order", userRequest,
                "appointments", available.isEmpty() ? "Not availables appointment" : available.toString()));

        log.info("Chain step 3 - appointments send to the model: {}", available);

        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("You are an assistant to manage medical appointments. You must response briefly and naturally.")
                .user(prompt)
                .call()
                .content();

    }

}
