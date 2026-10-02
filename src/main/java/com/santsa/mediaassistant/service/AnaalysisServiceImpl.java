package com.santsa.mediaassistant.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.santsa.mediaassistant.dto.analysis.ConditionSummary;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AnaalysisServiceImpl implements AnaalysisService {

    private final ChatClient geminiClient;

    private final ChatClient ollamaClient;

    public AnaalysisServiceImpl(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("ollamaClient") ChatClient ollamaClient) {
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
    }

    @Override
    public ConditionSummary summarizeCondition(String condition, String model) {

        log.info("Summarizing condition: {} using model: {}", condition, model);

        var converter = new BeanOutputConverter<>(ConditionSummary.class);
        String format = converter.getFormat();
        log.info("Using format: {}", format);

        String prompt = """
                Summarize the following medical condition in a concise manner, highlighting its key aspects, symptoms, and potential treatments.
            Return only one valid JSON object matching the format below. Do not add a preamble, markdown, emojis, or a footer.
            Write all string values in Spanish and do not include keys that are not in the format.
                Medical Condition: %s
                Format: %s
                """
                .formatted(condition, format);
        String jsonResponse = resolveClient(model)
                .prompt(prompt)
                .call()
                .content();
        log.info("Received response: {}", jsonResponse);

        return converter
                .convert(jsonResponse);

    }

    private ChatClient resolveClient(String model) {
        return "ollama".equalsIgnoreCase(model) ? ollamaClient : geminiClient;
    }

}
