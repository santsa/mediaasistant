package com.santsa.mediaassistant;

import java.util.Objects;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TestLlmCall implements CommandLineRunner {

    private final ChatModel chatModel;

    @Override
    public void run(String... args) throws Exception {
        ChatResponse chatResponse = chatModel.call(new Prompt("¿Qué es la fiebre?"));

        String content = Objects.requireNonNull(chatResponse.getResult()).getOutput().getText();
        System.out.println("=== Response ===");
        System.out.println(content);

        System.out.println("\n=== Metadata ===");
        System.out.println("Model" + chatResponse.getMetadata().getModel());
        System.out.println("Tokens de entrada: " + chatResponse.getMetadata().getUsage().getPromptTokens());
        System.out.println("Tokens de salida: " + chatResponse.getMetadata().getUsage().getCompletionTokens());
        System.out.println("Tokens totales: " + chatResponse.getMetadata().getUsage().getTotalTokens());
        System.out.println("Finish reason: " + chatResponse.getResult().getMetadata().getFinishReason());        
    }

}
