package com.santsa.mediaassistant.service;

import reactor.core.publisher.Flux;

public interface AssistantService {
    String chat(String prompt, String model, Long userId, String role);

    Flux<String> chatStream(String prompt, String model, Long userId, String role);

    String explainCondtion(String condition, String model);

    String analyzeSymptoms(String symptoms, String model);

    String diagnoseWithReasoning(String symptoms, String model);

    String consult(String query, String model);
}
