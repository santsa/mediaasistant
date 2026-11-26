package com.santsa.mediaassistant.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class ClientResolverDefaultTest {

    @Mock
    private ChatClient geminiClient;
    @Mock
    private ChatClient ollamaClient;
    @Mock
    private ChatModel geminiModel;
    @Mock
    private ChatModel ollamaModel;
    @Mock
    private VectorStore googleVectorStore;
    @Mock
    private VectorStore ollamaVectorStore;

    private ClientResolverDefault resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientResolverDefault(geminiClient, ollamaClient, geminiModel, ollamaModel,
                googleVectorStore, ollamaVectorStore);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "gemini", "GEMINI", "  Gemini  " })
    void resolve_whenGeminiOrNull_returnsGeminiBeans(String model) {
        assertThat(resolver.resolve(model)).isSameAs(geminiClient);
        assertThat(resolver.resolveModel(model)).isSameAs(geminiModel);
        assertThat(resolver.resolveVectorStore(model)).isSameAs(googleVectorStore);
    }

    @ParameterizedTest
    @ValueSource(strings = { "ollama", "OLLAMA", " Ollama " })
    void resolve_whenOllama_returnsOllamaBeans(String model) {
        assertThat(resolver.resolve(model)).isSameAs(ollamaClient);
        assertThat(resolver.resolveModel(model)).isSameAs(ollamaModel);
        assertThat(resolver.resolveVectorStore(model)).isSameAs(ollamaVectorStore);
    }

    @ParameterizedTest
    @ValueSource(strings = { "gpt-4", "", "   " })
    void resolve_whenUnsupportedModel_throwsIllegalArgumentException(String model) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.resolve(model))
                .withMessage("Not supported model: '%s'. Valid values: gemini, ollama".formatted(model));
    }

    @Test
    void resolveModel_whenUnsupportedModel_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() -> resolver.resolveModel("claude"));
    }

    @Test
    void resolveVectorStore_whenUnsupportedModel_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() -> resolver.resolveVectorStore("claude"));
    }
}
