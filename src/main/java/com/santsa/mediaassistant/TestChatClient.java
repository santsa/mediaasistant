package com.santsa.mediaassistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TestChatClient implements CommandLineRunner {

    private final ChatClient chatClient;

    @Override
    public void run(String... args) throws Exception {
        String response = chatClient
                .prompt("¿Qué es la fiebre")
                .call()
                .content();
        System.out.println(response);
    }

}
