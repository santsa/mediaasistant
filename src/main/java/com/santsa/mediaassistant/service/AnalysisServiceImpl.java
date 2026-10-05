package com.santsa.mediaassistant.service;

import java.util.List;
import java.util.Map;

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
    public ConditionSummary summarizeCondition(String condition, String model) {

        log.info("Summarizing condition: {} using model: {}", condition, model);

        return clientResolver.resolve(model)
                .prompt()
                .user("Give me a education medical resum about the following condition: " + condition)
                .call()
                .entity(ConditionSummary.class);

        /*
         * var converter = new BeanOutputConverter<>(ConditionSummary.class);
         * String format = converter.getFormat();
         * log.info("Using format: {}", format);
         * 
         * String prompt = """
         * Summarize the following medical condition in a concise manner, highlighting
         * its key aspects, symptoms, and potential treatments.
         * Return only one valid JSON object matching the format below. Do not add a
         * preamble, markdown, emojis, or a footer.
         * Write all string values in Spanish and do not include keys that are not in
         * the format.
         * Medical Condition: %s
         * Format: %s
         * """
         * .formatted(condition, format);
         * String jsonResponse = resolveClient(model)
         * .prompt(prompt)
         * .call()
         * .content();
         * log.info("Received response: {}", jsonResponse);
         * 
         * return converter
         * .convert(jsonResponse);
         */

    }

    @Override
    public List<ConditionSummary> listRelatedConditions(String symptoms, String model) {
        log.info("Listing related conditions for symptoms: {} using model: {}", symptoms, model);
        return clientResolver.resolve(model)
                .prompt()
                .user("List 3 related medical conditions for the following symptoms: " + symptoms)
                .call()
                .entity(new ParameterizedTypeReference<>() {
                });
    }

    @Override
    public SymptomAnalysis analyzeSymptoms(String symptoms, String model) {
        log.info("Analyzing symptoms: {} using model: {}", symptoms, model);

        String message = structuredAnalysisTemplate.render(Map.of("symptoms", symptoms));

        return clientResolver.resolve(model)
                .prompt()
                .user(message)
                .call()
                .entity(SymptomAnalysis.class);
    }

    @Override
    public QueryClassification classifyQuery(String query, String model) {
        log.info("Classification of query - moedl: {}", model);

        return clientResolver.resolve(model)
                .prompt()
                .user("Classify the following query of patient. " +
                        "Determine type of query: symptom report, general question, emergency, prescription request, or off-topic."
                        +
                        "Provide a brief explanation of the classification result, providing context or reasoning behind the classification. "
                        +
                        "Query: " + query)
                .call()
                .entity(QueryClassification.class);
    }

}
