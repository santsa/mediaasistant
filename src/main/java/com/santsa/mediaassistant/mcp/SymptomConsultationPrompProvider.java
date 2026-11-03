package com.santsa.mediaassistant.mcp;

import java.util.List;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class SymptomConsultationPrompProvider {

    @McpPrompt(name = "symptom-consultation", description = "Build a structured message to consult about medical symptoms")
    public McpSchema.GetPromptResult symptomConsultation(
            @McpArg(name = "symptom", description = "Main patient symptom", required = true) String symptom) {
        String message = """
                I need educational guidance on the following symptom: %s.

                Please include:
                - Possible common causes
                - When to consult a professional
                - Recommended medical specialty
                Remember that this information is for educational purposes only and does not replace
                an in-person medical consultation.
                """
                .formatted(symptom);

        return new McpSchema.GetPromptResult(
                "Inquiry regarding symptoms",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                        new McpSchema.TextContent(null, message, Map.of()))),
                Map.of());
    }

}
