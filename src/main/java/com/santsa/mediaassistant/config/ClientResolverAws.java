package com.santsa.mediaassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("aws")
public class ClientResolverAws implements ClientResolver {

    private final ChatClient geminiClient;

    private final ChatModel geminiModel;

    private final VectorStore googleVectorStore;

    private static final String GEMINI = "gemini";

    public ClientResolverAws(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("googleGenAiChatModel") ChatModel geminiModel,
            @Qualifier("googleVectorStore") VectorStore googleVectorStore) {
        this.geminiClient = geminiClient;
        this.geminiModel = geminiModel;
        this.googleVectorStore = googleVectorStore;
    }

    @Override
    public ChatClient resolve(String model) {
        assertGemini(model);
        return geminiClient;
    }

    @Override
    public ChatModel resolveModel(String model) {
        assertGemini(model);
        return geminiModel;
    }

    @Override
    public VectorStore resolveVectorStore(String model) {
        assertGemini(model);
        return googleVectorStore;
    }

    private void assertGemini(String model) {
        String normalized = model == null ? GEMINI : model.strip().toLowerCase();

        if (!normalized.equals(GEMINI)) {
            throw new IllegalArgumentException(
                    "Not supported model: '%s'. In AWS environment, only 'gemini' is supported.".formatted(model));
        }
    }

}