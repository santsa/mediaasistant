package com.santsa.mediaassistant.agent;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.config.MedicalAuditAdvisor;
import com.santsa.mediaassistant.dto.analysis.QueryClassification;
import com.santsa.mediaassistant.service.AnalysisService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingWorkflowServiceImpl implements RoutingWorkflowService {

    private final ClientResolver clientResolver;
    private final AnalysisService analysisService;
    private final MedicalAuditAdvisor medicalAuditAdvisor;

    @Value("classpath:prompts/routing-emergency.st")
    private Resource emergencyResource;

    @Value("classpath:prompts/routing-general.st")
    private Resource generalResource;

    @Value("classpath:prompts/routing-prescription.st")
    private Resource prescriptionResource;

    @Value("classpath:prompts/routing-symptom.st")
    private Resource symptomResource;

    private PromptTemplate emergencyTemplate;
    private PromptTemplate generalTemplate;
    private PromptTemplate prescriptionTemplate;
    private PromptTemplate symptomTemplate;

    @PostConstruct
    void init() {
        emergencyTemplate = new PromptTemplate(emergencyResource);
        generalTemplate = new PromptTemplate(generalResource);
        prescriptionTemplate = new PromptTemplate(prescriptionResource);
        symptomTemplate = new PromptTemplate(symptomResource);
    }

    @Override
    public String routeQuery(String query, String model, Long userId) {
        QueryClassification classification = analysisService.classifyQuery(query, model, userId);

        log.info("Router - type: {} - reason: {}", classification.type(), classification.reason());

        return switch (classification.type()) {
            case SYMPTOM_REPORT -> handleSymptomReport(query, model, userId);
            case GENERAL_QUESTION -> handleGeneralQuestion(query, model, userId);
            case EMERGENCY -> handleEmergency(query, model, userId);
            case PRESCRIPTION_REQUEST -> handlePrescriptionRequest(query, model, userId);
            case OFF_TOPIC -> handleOffTopic(userId);
        };
    }

    private String handleEmergency(String query, String model, Long userId) {
        log.info("Route: EMERGENCY");

        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("You are a specialyst in medical urgency. Must response directly and briefly.")
                .user(emergencyTemplate.render(Map.of("query", query)))
                .advisors(a -> a.advisors(medicalAuditAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .call()
                .content();
    }

    private String handleSymptomReport(String query, String model, Long userId) {
        log.info("Route: SYMPTOM_REPORT");

        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("You are a specialyst in symptom analisy.")
                .user(symptomTemplate.render(Map.of("query", query)))
                .advisors(a -> a.advisors(medicalAuditAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .call()
                .content();
    }

    private String handleGeneralQuestion(String query, String model, Long userId) {
        log.info("Route: GENERAL_QUESTION");

        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("You are an educational doctor. Response with detail and clarify.")
                .user(generalTemplate.render(Map.of("query", query)))
                .advisors(a -> a.advisors(medicalAuditAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .call()
                .content();
    }

    private String handlePrescriptionRequest(String query, String model, Long userId) {
        log.info("Route: PRESCRIPTION");

        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("You are a clinical pharmacologist, Mus inform about non-prescription medications.")
                .user(prescriptionTemplate.render(Map.of("query", query)))
                .advisors(a -> a.advisors(medicalAuditAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .call()
                .content();
    }

    private String handleOffTopic(Long userId) {
        log.info("Route: OFF_TOPIC -userId: {} - deterministic response, without call to model", userId);

        return "I think so, I can only help you with queries relatonship with healthy and mediccine. " +
                "I you have a medical query, you can write me";
    }

}
