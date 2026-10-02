package com.santsa.mediaassistant.service;

import java.util.Map;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.santsa.mediaassistant.config.ClientResolver;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    private final ClientResolver clientResolver;

    @Value("classpath:prompts/explain-condition.st")
    private Resource explainConditionPrompt;

    @Value("classpath:prompts/symptoms-analysis.st")
    private Resource symptomsAnalysisPrompt;

    @Value("classpath:prompts/diagnosis-cot.st")
    private Resource diagnosisWithReasoningPrompt;

    @Value("classpath:prompts/consult.st")
    private Resource consultPrompt;

    private PromptTemplate explainConditionTemplate;

    private PromptTemplate symptomsAnalysisTemplate;

    private PromptTemplate diagnosisWithReasoningTemplate;

    private PromptTemplate consultTemplate;

    @PostConstruct
    void init() {
        explainConditionTemplate = new PromptTemplate(explainConditionPrompt);
        symptomsAnalysisTemplate = new PromptTemplate(symptomsAnalysisPrompt);
        diagnosisWithReasoningTemplate = new PromptTemplate(diagnosisWithReasoningPrompt);
        consultTemplate = new PromptTemplate(consultPrompt);
    }

    @Override
    public String chat(String prompt, String model) {
        log.info("Chat with model: {} and prompt: {}", model, prompt);

        return clientResolver.resolve(model)
                .prompt(prompt)
                .call()
                .content();
    }

    @Override
    public Flux<String> chatStream(String prompt, String model) {
        log.info("Chat Streaming with model: {} and prompt: {}", model, prompt);

        return clientResolver.resolve(model)
                .prompt(prompt)
                .stream()
                .content();
    }

    @Override
    public String explainCondtion(String condition, String model) {
        log.info("Explaining condition: {} using model: {}", condition, model);

        String message = explainConditionTemplate.render(Map.of("condition", condition));

        return clientResolver.resolve(model)
                .prompt(message)
                .call()
                .content();
    }

    @Override
    public String analyzeSymptoms(String symptoms, String model) {
        log.info("Analyzing symptoms: {} using model: {}", symptoms, model);

        String message = symptomsAnalysisTemplate.render(Map.of("symptoms", symptoms));

        return clientResolver.resolve(model)
                .prompt(message)
                .call()
                .content();
    }

    @Override
    public String diagnoseWithReasoning(String symptoms, String model) {
        log.info("Diagnosing with reasoning for symptoms: {} using model: {}", symptoms, model);

        String message = diagnosisWithReasoningTemplate.render(Map.of("symptoms", symptoms));

        return clientResolver.resolve(model)
                .prompt(message)
                .call()
                .content();
    }

    @Override
    public String consult(String query, String model) {
        log.info("Consulting for query: {} using model: {}", query, model);

        String message = consultTemplate.render(Map.of("query", query));

        return clientResolver.resolve(model)
                .prompt(message)
                .call()
                .content();
    }

}
