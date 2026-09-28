package com.santsa.mediaassistant.chat;

import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatClient geminiClient;

    private final ChatClient ollamaClient;

    public ChatController(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("ollamaClient") ChatClient ollamaClient) {
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
    }

    @PostMapping("/prompt")
    public String prompt(
            @RequestBody String prompt,
            @RequestParam(defaultValue = "gemini") String model) {
        return resolveClient(model)
                .prompt(prompt)
                .call()
                .content();
    }

    @PostMapping(value = "/stream", produces = "text/event-stream; charset=UTF-8")
    public Flux<String> stream(
            @RequestBody String prompt,
            @RequestParam(defaultValue = "gemini") String model) {
        return resolveClient(model)
                .prompt(prompt)
                .stream()
                .content();
    }

    private ChatClient resolveClient(String model) {
        return "ollama".equalsIgnoreCase(model) ? ollamaClient : geminiClient;
    }

}
