package com.santsa.mediaassistant.service;

import reactor.core.publisher.Flux;

public interface AssistantService {
    String chat(String prompt, String model, Long userId, String role);

    Flux<String> chatStream(String prompt, String model, Long userId, String role);

    String explainCondtion(String condition, String model, Long userId);

    String analyzeSymptoms(String symptoms, String model, Long userId);

    String diagnoseWithReasoning(String symptoms, String mode, Long userIdl);

    String consult(String query, String model, Long userId);
}
