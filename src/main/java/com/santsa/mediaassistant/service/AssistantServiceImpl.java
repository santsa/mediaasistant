package com.santsa.mediaassistant.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AssistantServiceImpl implements AssistantService {

    private final ChatClient geminiClient;

    private final ChatClient ollamaClient;

    @Value("classpath:prompts/explain-condition.st")
    private Resource explainConditionPrompt;

    private PromptTemplate explainConditionTemplate;

    @PostConstruct 
    void init() {
        explainConditionTemplate = new PromptTemplate(explainConditionPrompt);
    }

    public AssistantServiceImpl(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("ollamaClient") ChatClient ollamaClient) {
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
    }

    @Override
    public String chat(String prompt, String model) {
        log.info("Chat with model: {} and prompt: {}", model, prompt);

        return resolveClient(model)
                .prompt(prompt)
                .call()
                .content();
    }

    @Override
    public Flux<String> chatStream(String prompt, String model) {
        log.info("Chat Streaming with model: {} and prompt: {}", model, prompt);

        return resolveClient(model)
                .prompt(prompt)
                .stream()
                .content();
    }

    private ChatClient resolveClient(String model) {
        return "ollama".equalsIgnoreCase(model) ? ollamaClient : geminiClient;
    }

    @Override
    public String explainCondtion(String condition, String model) {
        log.info("Explaining condition: {} using model: {}", condition, model);

        String message = explainConditionTemplate.render(Map.of("condition", condition));

        return resolveClient(model)
                .prompt(message)
                .call()
                .content();
    }

}
