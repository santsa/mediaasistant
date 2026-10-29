package com.santsa.mediaassistant.agent.orchestrator;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.santsa.mediaassistant.config.ClientResolver;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClinicalWorker {

    private final ClientResolver clientResolver;

    @Value("classpath:prompts/orchestrator-worker-clinical.st")
    private Resource clinicalResource;

    private PromptTemplate clinicalTemplate;

    @PostConstruct
    void init() {
        clinicalTemplate = new PromptTemplate(clinicalResource);
    }

    public String analyze(String symptoms, String model) {
        String prompt = clinicalTemplate.render(Map.of("symptoms", symptoms));

        String result = ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("You are an spacialyst in clinical reasoning.")
                .user(prompt)
                .call()
                .content();

        log.info("Clinical worker - completed analysis {} ", result);
        return result;
    }

}
