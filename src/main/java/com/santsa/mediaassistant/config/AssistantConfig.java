package com.santsa.mediaassistant.config;

import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

@Configuration
public class AssistantConfig {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    @Bean("geminiClient")
    ChatClient geminiClient(GoogleGenAiChatModel chatModel) throws IOException {
        return ChatClient.builder(chatModel)
        .defaultSystem(systemPromptResource.getContentAsString(StandardCharsets.UTF_8))
        .build();
    }

    @Bean
    @Profile("ollama")
    RestClientCustomizer ollamaBearerToken(@Value("${spring.ai.ollama.api-key}") String apiKey) {
        return builder -> builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
    }

    @Bean
    @Profile("ollama")
    WebClientCustomizer ollamaWebClientBearerToken(@Value("${spring.ai.ollama.api-key}") String apiKey) {
        return builder -> builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
    }

    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel chatModel) throws IOException {
        return ChatClient.builder(chatModel)
        .defaultSystem(systemPromptResource.getContentAsString(StandardCharsets.UTF_8))
        .build();
    }

}
