package com.santsa.mediaassistant.controller;

import reactor.core.publisher.Flux;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.santsa.mediaassistant.dto.ChatRequest;
import com.santsa.mediaassistant.service.AssistantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(
            @RequestBody ChatRequest chatRequest,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(assistantService.chat(chatRequest.prompt(), chatRequest.model(), userId));
    }

    @PostMapping(value = "/stream", produces = "text/event-stream; charset=UTF-8")
    public Flux<String> stream(
            @RequestBody ChatRequest chatRequest) {
        return assistantService.chatStream(chatRequest.prompt(), chatRequest.model());
    }

    @PostMapping("/explain")
    public ResponseEntity<String> explainCondition(
            @Valid @RequestBody ChatRequest chatRequest) {
        return ResponseEntity.ok(assistantService.explainCondtion(chatRequest.prompt(), chatRequest.model()));
    }

    @PostMapping("/symptoms")
    public ResponseEntity<String> analyzeSymptoms(
            @Valid @RequestBody ChatRequest chatRequest) {
        return ResponseEntity.ok(assistantService.analyzeSymptoms(chatRequest.prompt(), chatRequest.model()));
    }

    @PostMapping("/diagnose")
    public ResponseEntity<String> diagnoseWithReasoning(
            @Valid @RequestBody ChatRequest chatRequest) {
        return ResponseEntity.ok(assistantService.diagnoseWithReasoning(chatRequest.prompt(), chatRequest.model()));
    }

    @PostMapping("/consult")
    public ResponseEntity<String> consult(
            @Valid @RequestBody ChatRequest chatRequest) {
        return ResponseEntity.ok(assistantService.consult(chatRequest.prompt(), chatRequest.model()));
    }

}
