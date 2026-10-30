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
        return "ollama".equalsIgnoreCase(model) ? ollamaClient : geminiClient;
    }

    public ChatModel resolveModel(String model) {
        return "ollama".equalsIgnoreCase(model) ? ollamaModel : geminiModel;
    }

    public VectorStore resoleVectorStore(String model) {
        return "ollama".equalsIgnoreCase(model) ? ollamaVectorStore : googleVectorStore;
    }

}
