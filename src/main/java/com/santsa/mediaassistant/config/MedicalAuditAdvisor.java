package com.santsa.mediaassistant.config;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class MedicalAuditAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String NAME_MEDICAL_AUDIT_ADVISOR = "MedicalAuditAdvisor";

    @Override
    public String getName() {
        return NAME_MEDICAL_AUDIT_ADVISOR;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
            StreamAdvisorChain streamAdvisorChain) {
        String userMessage = chatClientRequest.prompt().getUserMessage().getText();
        String conversationId = chatClientRequest.context()
                .getOrDefault(ChatMemory.CONVERSATION_ID, "unknown").toString();

        log.info("AUDIT | conversationId={} | question={}",
                conversationId, userMessage);

        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        String userMessage = chatClientRequest.prompt().getUserMessage().getText();
        String conversationId = chatClientRequest.context()
                .getOrDefault(ChatMemory.CONVERSATION_ID, "unknown").toString();

        log.info("AUDIT | conversationId={} | question={}",
                conversationId, userMessage);

        long start = System.currentTimeMillis();
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
        long elapsed = System.currentTimeMillis() - start;

        String content = response.chatResponse()
                .getResult().getOutput().getText();
        String preview = content != null && content.length() > 100
                ? content.substring(0, 100) + "..."
                : content;

        log.info("AUDIT | conversationId={} | time={}ms | response={}",
                conversationId, elapsed, preview);

        return response;
    }

}
