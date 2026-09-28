package com.santsa.mediaassistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Profile;

@Configuration
public class AssistantConfig {

    @Bean("geminiClient")
    ChatClient geminiClient(GoogleGenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
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
    ChatClient ollamaClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

}
