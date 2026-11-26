package com.santsa.mediaassistant.controller;

import static com.santsa.mediaassistant.controller.ControllerTestSupport.USER_ID;
import static com.santsa.mediaassistant.controller.ControllerTestSupport.body;
import static com.santsa.mediaassistant.controller.ControllerTestSupport.patient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.santsa.mediaassistant.config.SecurityConfig;
import com.santsa.mediaassistant.dto.analysis.ConditionSummary;
import com.santsa.mediaassistant.dto.analysis.PosibleCondition;
import com.santsa.mediaassistant.dto.analysis.QueryClassification;
import com.santsa.mediaassistant.dto.analysis.QueryType;
import com.santsa.mediaassistant.dto.analysis.Severity;
import com.santsa.mediaassistant.dto.analysis.SymptomAnalysis;
import com.santsa.mediaassistant.dto.analysis.Urgency;
import com.santsa.mediaassistant.service.AnalysisService;

@WebMvcTest(AnalysisController.class)
@Import(SecurityConfig.class)
class AnalysisControllerTest {

    private static final ConditionSummary ASTHMA = new ConditionSummary("Asthma", "Chronic airway inflammation",
            "Wheezing", Severity.MODERATE, Urgency.MEDIUM, "If breathing gets worse");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisService analysisService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void condition_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/condition").contentType(MediaType.APPLICATION_JSON).content(body("asthma")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(analysisService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/v1/analysis/condition", "/api/v1/analysis/conditions",
            "/api/v1/analysis/symptoms", "/api/v1/analysis/classify" })
    void anyEndpoint_withBlankPrompt_returns400WithoutCallingService(String path) throws Exception {
        mockMvc.perform(post(path).with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(analysisService);
    }

    @Test
    void condition_returnsConditionSummaryAsJson() throws Exception {
        when(analysisService.summarizeCondition("asthma", "gemini", USER_ID)).thenReturn(ASTHMA);

        mockMvc.perform(post("/api/v1/analysis/condition").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("asthma")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionName").value("Asthma"))
                .andExpect(jsonPath("$.severity").value("MODERATE"))
                .andExpect(jsonPath("$.urgency").value("MEDIUM"));
    }

    @Test
    void conditions_returnsJsonArray() throws Exception {
        when(analysisService.listRelatedConditions("cough", "gemini", USER_ID)).thenReturn(List.of(ASTHMA, ASTHMA));

        mockMvc.perform(post("/api/v1/analysis/conditions").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("cough")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].conditionName").value("Asthma"));
    }

    @Test
    void symptoms_returnsSymptomAnalysisAsJson() throws Exception {
        when(analysisService.analyzeSymptoms("fever", "gemini", USER_ID)).thenReturn(new SymptomAnalysis(
                List.of("fever"), List.of(new PosibleCondition("Flu", "Viral", Severity.MILD)), Urgency.LOW, "Rest"));

        mockMvc.perform(post("/api/v1/analysis/symptoms").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("fever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifyedSymptoms[0]").value("fever"))
                .andExpect(jsonPath("$.possibleConditions[0].name").value("Flu"))
                .andExpect(jsonPath("$.urgencyLevel").value("LOW"));
    }

    @Test
    void classify_returnsClassificationAsJson() throws Exception {
        when(analysisService.classifyQuery("chest pain", "gemini", USER_ID))
                .thenReturn(new QueryClassification(QueryType.EMERGENCY, "Chest pain is urgent"));

        mockMvc.perform(post("/api/v1/analysis/classify").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("chest pain")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("EMERGENCY"))
                .andExpect(jsonPath("$.reason").value("Chest pain is urgent"));
    }
}
