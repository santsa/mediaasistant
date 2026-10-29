package com.santsa.mediaassistant.agent.orchestrator;

public interface OrchestratorService {

    String orchestrate(String symptoms, String model, Long userId);
}
