package com.santsa.mediaassistant.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.Ordered;

import com.santsa.mediaassistant.support.FakeChatModel;

class MedicalAuditAdvisorTest {

    private final MedicalAuditAdvisor advisor = new MedicalAuditAdvisor();

    @Test
    void hasNameAndHighestPrecedence() {
        assertThat(advisor.getName()).isEqualTo("MedicalAuditAdvisor");
        assertThat(advisor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void adviseCall_passesRequestAndResponseThroughUnchanged() {
        FakeChatModel model = new FakeChatModel("x".repeat(250));
        ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

        String result = client.prompt().user("question")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "7"))
                .call().content();

        assertThat(result).isEqualTo("x".repeat(250));
        assertThat(model.lastUserText()).isEqualTo("question");
    }

    @Test
    void adviseCall_withoutConversationIdAndShortContent_doesNotFail() {
        FakeChatModel model = new FakeChatModel("short");
        ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

        String result = client.prompt().user("question").call().content();

        assertThat(result).isEqualTo("short");
    }

    @Test
    void adviseStream_passesChunksThroughUnchanged() {
        FakeChatModel model = new FakeChatModel().streaming("a", "b", "c");
        ChatClient client = ChatClient.builder(model).defaultAdvisors(advisor).build();

        List<String> chunks = client.prompt().user("question").stream().content().collectList().block();

        assertThat(chunks).containsExactly("a", "b", "c");
    }
}
