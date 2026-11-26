package com.santsa.mediaassistant.support;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import reactor.core.publisher.Flux;

/**
 * In-memory {@link ChatModel} for unit tests: records every {@link Prompt} it receives and answers with
 * canned responses, in order (the last one is repeated). Lets tests drive a real {@link
 * org.springframework.ai.chat.client.ChatClient} — prompt rendering, advisors, memory, structured output —
 * without calling any LLM.
 */
public class FakeChatModel implements ChatModel {

    private final Deque<String> responses = new ArrayDeque<>();
    private final List<Prompt> prompts = new ArrayList<>();
    private List<String> streamChunks = List.of();

    public FakeChatModel(String... responses) {
        this.responses.addAll(Arrays.asList(responses));
    }

    /** Chunks emitted, one {@link ChatResponse} each, by {@link #stream(Prompt)}. */
    public FakeChatModel streaming(String... chunks) {
        this.streamChunks = List.of(chunks);
        return this;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        String text = responses.size() > 1 ? responses.poll() : responses.peek();
        return response(text);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        prompts.add(prompt);
        return Flux.fromIterable(streamChunks).map(FakeChatModel::response);
    }

    /** Tool-calling options so ChatClient keeps tools and toolContext in the Prompt (inspectable below). */
    @Override
    public ChatOptions getOptions() {
        return ToolCallingChatOptions.builder().build();
    }

    private static ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    // --- inspection helpers ---

    public List<Prompt> prompts() {
        return prompts;
    }

    public int callCount() {
        return prompts.size();
    }

    public Prompt lastPrompt() {
        return prompts.getLast();
    }

    public String lastUserText() {
        return lastPrompt().getUserMessage().getText();
    }

    public String lastSystemText() {
        return lastPrompt().getSystemMessage().getText();
    }

    public List<Message> lastMessages() {
        return lastPrompt().getInstructions();
    }

    public Map<String, Object> lastToolContext() {
        return ((ToolCallingChatOptions) lastPrompt().getOptions()).getToolContext();
    }

    public List<String> lastToolNames() {
        return ((ToolCallingChatOptions) lastPrompt().getOptions()).getToolCallbacks().stream()
                .map(cb -> cb.getToolDefinition().name())
                .toList();
    }
}
