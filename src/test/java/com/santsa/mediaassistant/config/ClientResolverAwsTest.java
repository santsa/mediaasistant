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
class ClientResolverAwsTest {

    @Mock
    private ChatClient geminiClient;

    @Mock
    private ChatModel geminiModel;

    @Mock
    private VectorStore googleVectorStore;

    private ClientResolverAws resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientResolverAws(geminiClient, geminiModel, googleVectorStore);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "gemini", "GEMINI", "  Gemini  " })
    void resolve_whenModelIsGeminiOrNull_returnsGeminiClient(String model) {
        assertThat(resolver.resolve(model)).isSameAs(geminiClient);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "gemini", "GEMINI", "  Gemini  " })
    void resolveModel_whenModelIsGeminiOrNull_returnsGeminiModel(String model) {
        assertThat(resolver.resolveModel(model)).isSameAs(geminiModel);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "gemini", "GEMINI", "  Gemini  " })
    void resolveVectorStore_whenModelIsGeminiOrNull_returnsGoogleVectorStore(String model) {
        assertThat(resolver.resolveVectorStore(model)).isSameAs(googleVectorStore);
    }

    @ParameterizedTest
    @ValueSource(strings = { "ollama", "gpt-4", "", "   " })
    void resolve_whenModelIsNotGemini_throwsIllegalArgumentException(String model) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.resolve(model))
                .withMessageContaining("Not supported model: '%s'".formatted(model))
                .withMessageContaining("only 'gemini' is supported");
    }

    @Test
    void resolveModel_whenModelIsOllama_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() -> resolver.resolveModel("ollama"));
    }

    @Test
    void resolveVectorStore_whenModelIsOllama_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() -> resolver.resolveVectorStore("ollama"));
    }
}
