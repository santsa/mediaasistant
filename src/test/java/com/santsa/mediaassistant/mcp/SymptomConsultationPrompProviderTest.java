package com.santsa.mediaassistant.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.spec.McpSchema;

class SymptomConsultationPrompProviderTest {

    private final SymptomConsultationPrompProvider provider = new SymptomConsultationPrompProvider();

    @Test
    void symptomConsultation_buildsSingleUserMessageWithSymptomAndDisclaimer() {
        McpSchema.GetPromptResult result = provider.symptomConsultation("migraine");

        assertThat(result.description()).isEqualTo("Inquiry regarding symptoms");
        assertThat(result.messages()).hasSize(1);
        McpSchema.PromptMessage message = result.messages().getFirst();
        assertThat(message.role()).isEqualTo(McpSchema.Role.USER);
        assertThat(((McpSchema.TextContent) message.content()).text())
                .contains("following symptom: migraine.")
                .contains("Recommended medical specialty")
                .contains("does not replace");
    }
}
