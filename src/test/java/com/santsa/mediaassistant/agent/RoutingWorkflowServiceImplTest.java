package com.santsa.mediaassistant.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.config.MedicalAuditAdvisor;
import com.santsa.mediaassistant.dto.analysis.QueryClassification;
import com.santsa.mediaassistant.dto.analysis.QueryType;
import com.santsa.mediaassistant.service.AnalysisService;
import com.santsa.mediaassistant.support.FakeChatModel;

@ExtendWith(MockitoExtension.class)
class RoutingWorkflowServiceImplTest {

    @Mock
    private ClientResolver clientResolver;

    @Mock
    private AnalysisService analysisService;

    private RoutingWorkflowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoutingWorkflowServiceImpl(clientResolver, analysisService, new MedicalAuditAdvisor());
        ReflectionTestUtils.setField(service, "emergencyResource", new ClassPathResource("prompts/routing-emergency.st"));
        ReflectionTestUtils.setField(service, "generalResource", new ClassPathResource("prompts/routing-general.st"));
        ReflectionTestUtils.setField(service, "prescriptionResource", new ClassPathResource("prompts/routing-prescription.st"));
        ReflectionTestUtils.setField(service, "symptomResource", new ClassPathResource("prompts/routing-symptom.st"));
        service.init();
    }

    private void classifyAs(QueryType type) {
        when(analysisService.classifyQuery("my query", "gemini", 1L))
                .thenReturn(new QueryClassification(type, "test reason"));
    }

    static Stream<Arguments> modelRoutes() {
        return Stream.of(
                Arguments.of(QueryType.EMERGENCY, "specialyst in medical urgency", "URGENCIA MÉDICA"),
                Arguments.of(QueryType.SYMPTOM_REPORT, "specialyst in symptom analisy", "A patient reports symptoms"),
                Arguments.of(QueryType.GENERAL_QUESTION, "educational doctor", "An educational medical inquiry"),
                Arguments.of(QueryType.PRESCRIPTION_REQUEST, "clinical pharmacologist",
                        "A patient makes an inquiry related to medication"));
    }

    @ParameterizedTest
    @MethodSource("modelRoutes")
    void routeQuery_dispatchesToSystemPromptAndTemplateOfClassifiedType(QueryType type, String expectedSystem,
            String expectedTemplateStart) {
        classifyAs(type);
        FakeChatModel model = new FakeChatModel("routed answer");
        when(clientResolver.resolveModel("gemini")).thenReturn(model);

        String result = service.routeQuery("my query", "gemini", 1L);

        assertThat(result).isEqualTo("routed answer");
        assertThat(model.lastSystemText()).contains(expectedSystem);
        assertThat(model.lastUserText()).startsWith(expectedTemplateStart).contains("my query");
    }

    @Test
    void routeQuery_whenOffTopic_returnsFixedAnswerWithoutCallingModel() {
        classifyAs(QueryType.OFF_TOPIC);

        String result = service.routeQuery("my query", "gemini", 1L);

        assertThat(result).startsWith("I think so, I can only help you with queries relatonship with healthy");
        verifyNoInteractions(clientResolver);
    }
}
