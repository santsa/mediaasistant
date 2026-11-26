package com.santsa.mediaassistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.dto.analysis.ConditionSummary;
import com.santsa.mediaassistant.dto.analysis.PosibleCondition;
import com.santsa.mediaassistant.dto.analysis.QueryClassification;
import com.santsa.mediaassistant.dto.analysis.QueryType;
import com.santsa.mediaassistant.dto.analysis.Severity;
import com.santsa.mediaassistant.dto.analysis.SymptomAnalysis;
import com.santsa.mediaassistant.dto.analysis.Urgency;
import com.santsa.mediaassistant.support.FakeChatModel;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceImplTest {

    private static final String CONDITION_JSON = """
            {"conditionName":"Asthma","description":"Chronic airway inflammation",
             "commonSymptoms":"Wheezing","severity":"MODERATE","urgency":"MEDIUM",
             "whenToSeeDoctor":"If breathing gets worse"}
            """;

    private static final ConditionSummary ASTHMA = new ConditionSummary("Asthma", "Chronic airway inflammation",
            "Wheezing", Severity.MODERATE, Urgency.MEDIUM, "If breathing gets worse");

    @Mock
    private ClientResolver clientResolver;

    private AnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnalysisServiceImpl(clientResolver);
        ReflectionTestUtils.setField(service, "structuredAnalysisPrompt",
                new ClassPathResource("prompts/structured-analysis.st"));
        service.init();
    }

    private FakeChatModel clientAnswering(String json) {
        FakeChatModel model = new FakeChatModel(json);
        when(clientResolver.resolve("gemini")).thenReturn(ChatClient.builder(model).build());
        return model;
    }

    @Test
    void summarizeCondition_mapsModelJsonToConditionSummary() {
        FakeChatModel model = clientAnswering(CONDITION_JSON);

        ConditionSummary result = service.summarizeCondition("asthma", "gemini", 1L);

        assertThat(result).isEqualTo(ASTHMA);
        assertThat(model.lastUserText())
                .startsWith("Give me a education medical resum about the following condition: asthma");
    }

    @Test
    void summarizeCondition_asksModelForJsonFormat() {
        FakeChatModel model = clientAnswering(CONDITION_JSON);

        service.summarizeCondition("asthma", "gemini", 1L);

        assertThat(model.lastUserText()).contains("conditionName").contains("whenToSeeDoctor");
    }

    @Test
    void listRelatedConditions_mapsJsonArrayToList() {
        FakeChatModel model = clientAnswering("[" + CONDITION_JSON + "," + CONDITION_JSON + "]");

        List<ConditionSummary> result = service.listRelatedConditions("cough", "gemini", 1L);

        assertThat(result).containsExactly(ASTHMA, ASTHMA);
        assertThat(model.lastUserText()).startsWith("List 3 related medical conditions for the following symptoms: cough");
    }

    @Test
    void analyzeSymptoms_rendersStructuredTemplateAndMapsToSymptomAnalysis() {
        FakeChatModel model = clientAnswering("""
                {"identifyedSymptoms":["fever","cough"],
                 "possibleConditions":[{"name":"Flu","description":"Viral infection","severity":"MILD"}],
                 "urgencyLevel":"LOW","recommendation":"Rest and fluids"}
                """);

        SymptomAnalysis result = service.analyzeSymptoms("fever and cough", "gemini", 1L);

        assertThat(result).isEqualTo(new SymptomAnalysis(List.of("fever", "cough"),
                List.of(new PosibleCondition("Flu", "Viral infection", Severity.MILD)), Urgency.LOW,
                "Rest and fluids"));
        assertThat(model.lastUserText()).contains("\"fever and cough\"");
    }

    @Test
    void classifyQuery_usesChatModelOfResolverAndMapsClassification() {
        FakeChatModel model = new FakeChatModel("{\"type\":\"EMERGENCY\",\"reason\":\"Chest pain\"}");
        when(clientResolver.resolveModel("ollama")).thenReturn(model);

        QueryClassification result = service.classifyQuery("I have chest pain", "ollama", 1L);

        assertThat(result).isEqualTo(new QueryClassification(QueryType.EMERGENCY, "Chest pain"));
        assertThat(model.lastUserText()).contains("Query of the patient: I have chest pain");
        verify(clientResolver).resolveModel("ollama");
    }
}
