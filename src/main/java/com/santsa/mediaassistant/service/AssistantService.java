package com.santsa.mediaassistant.service;

import reactor.core.publisher.Flux;

public interface AssistantService {
    String chat(String prompt, String model);
    Flux<String> chatStream(String prompt, String model);
    String explainCondtion(String condition, String model);
    String analyzeSymptoms(String symptoms, String model);
    String diagnoseWithReasoning(String symptoms, String model);
}
