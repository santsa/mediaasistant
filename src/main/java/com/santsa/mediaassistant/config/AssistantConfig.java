package com.santsa.mediaassistant.config;

import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import com.santsa.mediaassistant.tools.AppointmentSearchTool;
import com.santsa.mediaassistant.tools.DoctorInfoTool;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

@Configuration
@RequiredArgsConstructor
public class AssistantConfig {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    private final AppointmentSearchTool appointmentSearchTool;
    private final DoctorInfoTool doctorInfoTool;

    @Bean("geminiClient")
    ChatClient geminiClient(GoogleGenAiChatModel chatModel) throws IOException {

        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool, doctorInfoTool)
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

        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool, doctorInfoTool)
                .build();
    }

}
