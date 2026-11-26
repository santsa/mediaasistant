package com.santsa.mediaassistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.support.FakeChatModel;

@ExtendWith(MockitoExtension.class)
class AssistantServiceImplTest {

    @Mock
    private ClientResolver clientResolver;

    private ChatMemory chatMemory;
    private FakeChatModel chatModel;
    private AssistantServiceImpl service;

    @BeforeEach
    void setUp() {
        chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();
        chatModel = new FakeChatModel("model answer").streaming("Hel", "lo");
        service = new AssistantServiceImpl(clientResolver, chatMemory);
        ReflectionTestUtils.setField(service, "explainConditionPrompt", new ClassPathResource("prompts/explain-condition.st"));
        ReflectionTestUtils.setField(service, "symptomsAnalysisPrompt", new ClassPathResource("prompts/symptoms-analysis.st"));
        ReflectionTestUtils.setField(service, "diagnosisWithReasoningPrompt", new ClassPathResource("prompts/diagnosis-cot.st"));
        ReflectionTestUtils.setField(service, "consultPrompt", new ClassPathResource("prompts/consult.st"));
        service.init();
    }

    private void resolveTo(String model) {
        when(clientResolver.resolve(model)).thenReturn(ChatClient.builder(chatModel).build());
    }

    // --- chat ---

    @Test
    void chat_sendsPromptAndReturnsModelContent() {
        resolveTo("gemini");

        String result = service.chat("Hola", "gemini", 1L, "PATIENT");

        assertThat(result).isEqualTo("model answer");
        assertThat(chatModel.lastUserText()).isEqualTo("Hola");
    }

    @Test
    void chat_passesUserIdAndRoleAsToolContext() {
        resolveTo("ollama");

        service.chat("Hola", "ollama", 7L, "ADMIN");

        assertThat(chatModel.lastToolContext()).containsEntry("userId", 7L).containsEntry("role", "ADMIN");
    }

    @Test
    void chat_storesConversationInMemoryKeyedByUserId() {
        resolveTo("gemini");

        service.chat("first question", "gemini", 1L, "PATIENT");

        List<Message> stored = chatMemory.get("1");
        assertThat(stored).extracting(Message::getText).containsExactly("first question", "model answer");
        assertThat(chatMemory.get("2")).isEmpty();
    }

    @Test
    void chat_includesPreviousTurnsOfSameUserInNextPrompt() {
        resolveTo("gemini");

        service.chat("first question", "gemini", 1L, "PATIENT");
        service.chat("second question", "gemini", 1L, "PATIENT");

        assertThat(chatModel.lastMessages()).extracting(Message::getText)
                .containsSubsequence("first question", "model answer", "second question");
    }

    @Test
    void chat_doesNotLeakHistoryBetweenUsers() {
        resolveTo("gemini");

        service.chat("secret of user 1", "gemini", 1L, "PATIENT");
        service.chat("question of user 2", "gemini", 2L, "PATIENT");

        assertThat(chatModel.lastMessages()).extracting(Message::getText)
                .doesNotContain("secret of user 1");
    }

    // --- chatStream ---

    @Test
    void chatStream_emitsModelChunksAndPassesToolContext() {
        resolveTo("gemini");

        List<String> chunks = service.chatStream("Hola", "gemini", 3L, "PATIENT").collectList().block();

        assertThat(chunks).containsExactly("Hel", "lo");
        assertThat(chatModel.lastToolContext()).containsEntry("userId", 3L).containsEntry("role", "PATIENT");
    }

    // --- template-based methods ---

    @Test
    void explainCondtion_rendersExplainTemplateWithCondition() {
        resolveTo("gemini");

        String result = service.explainCondtion("asthma", "gemini", 1L);

        assertThat(result).isEqualTo("model answer");
        assertThat(chatModel.lastUserText()).startsWith("Explain the following asthma");
    }

    @Test
    void analyzeSymptoms_rendersSymptomsTemplate() {
        resolveTo("gemini");

        service.analyzeSymptoms("headache and fever", "gemini", 1L);

        assertThat(chatModel.lastUserText())
                .startsWith("You are an educational medical assistant. When a patient describes symptoms")
                .contains("headache and fever");
    }

    @Test
    void diagnoseWithReasoning_rendersChainOfThoughtTemplate() {
        resolveTo("gemini");

        service.diagnoseWithReasoning("chest pain", "gemini", 1L);

        assertThat(chatModel.lastUserText()).contains("\"chest pain\"");
    }

    @Test
    void consult_rendersConsultTemplateWithQuery() {
        resolveTo("gemini");

        service.consult("Is ibuprofen safe?", "gemini", 1L);

        assertThat(chatModel.lastUserText())
                .startsWith("Analyze the following patient's inquiry")
                .contains("Is ibuprofen safe?");
    }

    @Test
    void templateMethods_doNotWriteToChatMemory() {
        resolveTo("gemini");

        service.explainCondtion("asthma", "gemini", 1L);

        assertThat(chatMemory.get("1")).isEmpty();
    }
}
