package com.santsa.mediaassistant.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.dto.analysis.ConditionSummary;
import com.santsa.mediaassistant.dto.analysis.QueryClassification;
import com.santsa.mediaassistant.dto.analysis.SymptomAnalysis;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final ClientResolver clientResolver;

    @Value("classpath:prompts/structured-analysis.st")
    private Resource structuredAnalysisPrompt;

    private PromptTemplate structuredAnalysisTemplate;

    @PostConstruct
    void init() {
        structuredAnalysisTemplate = new PromptTemplate(structuredAnalysisPrompt);
    }

    @Override
    public ConditionSummary summarizeCondition(String condition, String model, Long userId) {

        log.info("Summarizing condition: {} using model: {}", condition, model);

        return clientResolver.resolve(model)
                .prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .user("Give me a education medical resum about the following condition: " + condition)
                .call()
                .entity(ConditionSummary.class);

    }

    @Override
    public List<ConditionSummary> listRelatedConditions(String symptoms, String model, Long userId) {
        log.info("Listing related conditions for symptoms: {} using model: {}", symptoms, model);
        return clientResolver.resolve(model)
                .prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .user("List 3 related medical conditions for the following symptoms: " + symptoms)
                .call()
                .entity(new ParameterizedTypeReference<>() {
                });
    }

    @Override
    public SymptomAnalysis analyzeSymptoms(String symptoms, String model, Long userId) {
        log.info("Analyzing symptoms: {} using model: {}", symptoms, model);

        String message = structuredAnalysisTemplate.render(Map.of("symptoms", symptoms));

        return clientResolver.resolve(model)
                .prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .user(message)
                .call()
                .entity(SymptomAnalysis.class);
    }

    @Override
    public QueryClassification classifyQuery(String query, String model, Long userId) {
        log.info("Classification of query - moedl: {}", model);

        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .user("Classify the following query of patient. " +
                        "Determine type of query. "
                        +
                        "Provide a brief explanation of the classification resul. "
                        +
                        "Query of the patient: " + query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .call()
                .entity(QueryClassification.class);
    }

}
