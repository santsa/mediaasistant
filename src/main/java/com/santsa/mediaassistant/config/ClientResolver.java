package com.santsa.mediaassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ClientResolver {

    private final ChatClient geminiClient;

    private final ChatClient ollamaClient;

    private final ChatModel geminiModel;

    private final ChatModel ollamaModel;

    private final VectorStore googleVectorStore;

    private final VectorStore ollamaVectorStore;

    private static final String GEMINI = "gemini";

    private static final String OLLAMA = "ollama";

    public ClientResolver(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("ollamaClient") ChatClient ollamaClient,
            @Qualifier("googleGenAiChatModel") ChatModel geminiModel,
            @Qualifier("ollamaChatModel") ChatModel ollamaModel,
            @Qualifier("googleVectorStore") VectorStore googleVectorStore,
            @Qualifier("ollamaVectorStore") VectorStore ollamaVectorStore) {
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
        this.geminiModel = geminiModel;
        this.ollamaModel = ollamaModel;
        this.googleVectorStore = googleVectorStore;
        this.ollamaVectorStore = ollamaVectorStore;
    }

    public ChatClient resolve(String model) {
        return switch (normalize(model)) {
            case GEMINI -> geminiClient;
            case OLLAMA -> ollamaClient;
            default -> throw new IllegalArgumentException(
                    "Not supported model: '%s'. Valid values: gemini, ollama".formatted(model));
        };
    }

    public ChatModel resolveModel(String model) {
        return switch (normalize(model)) {
            case GEMINI -> geminiModel;
            case OLLAMA -> ollamaModel;
            default -> throw new IllegalArgumentException(
                    "Not supported model: '%s'. Valid values: gemini, ollama".formatted(model));
        };
    }

    public VectorStore resolveVectorStore(String model) {
        return switch (normalize(model)) {
            case GEMINI -> googleVectorStore;
            case OLLAMA -> ollamaVectorStore;
            default -> throw new IllegalArgumentException(
                    "Not supported model: '%s'. Valid values: gemini, ollama".formatted(model));
        };
    }

    
    private String normalize(String model) {
        return model == null ? "gemini" : model.strip().toLowerCase();
    }

}
