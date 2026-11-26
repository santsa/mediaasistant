package com.santsa.mediaassistant.controller;

import static com.santsa.mediaassistant.controller.ControllerTestSupport.USER_ID;
import static com.santsa.mediaassistant.controller.ControllerTestSupport.admin;
import static com.santsa.mediaassistant.controller.ControllerTestSupport.body;
import static com.santsa.mediaassistant.controller.ControllerTestSupport.patient;
import static com.santsa.mediaassistant.controller.ControllerTestSupport.userWithoutRole;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MvcResult;

import com.santsa.mediaassistant.config.SecurityConfig;
import com.santsa.mediaassistant.service.AssistantService;
import com.santsa.mediaassistant.util.SecurityUtils;

import reactor.core.publisher.Flux;

@WebMvcTest(ChatController.class)
@Import({ SecurityConfig.class, SecurityUtils.class })
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistantService assistantService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // --- security ---

    @Test
    void chat_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/chat").contentType(MediaType.APPLICATION_JSON).content(body("hi")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(assistantService);
    }

    @Test
    void chat_withTokenButNoPatientOrAdminRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/chat").with(userWithoutRole())
                .contentType(MediaType.APPLICATION_JSON).content(body("hi")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(assistantService);
    }

    @Test
    void nonApiPath_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/anything-else")).andExpect(status().isUnauthorized());
    }

    @Test
    void mcpPath_isPublic_soItReachesDispatcherWithoutToken() throws Exception {
        // No MCP endpoint exists in this slice: 404 (not 401) proves /mcp/** is permitAll.
        mockMvc.perform(post("/mcp/anything")).andExpect(status().isNotFound());
    }

    // --- /chat ---

    @Test
    void chat_asPatient_passesUserIdFromJwtAndPatientRole() throws Exception {
        when(assistantService.chat("hi", "gemini", USER_ID, "PATIENT")).thenReturn("hello patient");

        mockMvc.perform(post("/api/v1/chat").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("hi")))
                .andExpect(status().isOk())
                .andExpect(content().string("hello patient"));
    }

    @Test
    void chat_asAdmin_passesAdminRole() throws Exception {
        when(assistantService.chat("hi", "gemini", USER_ID, "ADMIN")).thenReturn("hello admin");

        mockMvc.perform(post("/api/v1/chat").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content(body("hi")))
                .andExpect(status().isOk())
                .andExpect(content().string("hello admin"));
    }

    @Test
    void chat_withoutModel_passesNullModelToService() throws Exception {
        when(assistantService.chat("hi", null, USER_ID, "PATIENT")).thenReturn("ok");

        mockMvc.perform(post("/api/v1/chat").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content("{\"prompt\":\"hi\"}"))
                .andExpect(status().isOk());

        verify(assistantService).chat("hi", null, USER_ID, "PATIENT");
    }

    @Test
    void chat_whenModelUnsupported_returns400WithMessage() throws Exception {
        when(assistantService.chat(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Not supported model: 'gpt'"));

        mockMvc.perform(post("/api/v1/chat").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content("{\"prompt\":\"hi\",\"model\":\"gpt\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Not supported model: 'gpt'"));
    }

    // --- /stream ---

    @Test
    void stream_returnsServiceChunksAsServerSentEvents() throws Exception {
        when(assistantService.chatStream("hi", "gemini", USER_ID, "PATIENT")).thenReturn(Flux.just("Hel", "lo"));

        MvcResult result = mockMvc.perform(post("/api/v1/stream").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("hi")))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("data:Hel")))
                .andExpect(content().string(containsString("data:lo")));
    }

    // --- template endpoints ---

    @Test
    void explain_delegatesToExplainCondition() throws Exception {
        when(assistantService.explainCondtion("asthma", "gemini", USER_ID)).thenReturn("explained");

        mockMvc.perform(post("/api/v1/explain").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("asthma")))
                .andExpect(status().isOk())
                .andExpect(content().string("explained"));
    }

    @Test
    void symptoms_delegatesToAnalyzeSymptoms() throws Exception {
        when(assistantService.analyzeSymptoms("fever", "gemini", USER_ID)).thenReturn("analysis");

        mockMvc.perform(post("/api/v1/symptoms").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("fever")))
                .andExpect(status().isOk())
                .andExpect(content().string("analysis"));
    }

    @Test
    void diagnose_delegatesToDiagnoseWithReasoning() throws Exception {
        when(assistantService.diagnoseWithReasoning("chest pain", "gemini", USER_ID)).thenReturn("diagnosis");

        mockMvc.perform(post("/api/v1/diagnose").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("chest pain")))
                .andExpect(status().isOk())
                .andExpect(content().string("diagnosis"));
    }

    @Test
    void consult_delegatesToConsult() throws Exception {
        when(assistantService.consult("ibuprofen?", "gemini", USER_ID)).thenReturn("consulted");

        mockMvc.perform(post("/api/v1/consult").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("ibuprofen?")))
                .andExpect(status().isOk())
                .andExpect(content().string("consulted"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/v1/chat", "/api/v1/stream", "/api/v1/explain", "/api/v1/symptoms",
            "/api/v1/diagnose", "/api/v1/consult" })
    void anyEndpoint_withBlankPrompt_returns400WithoutCallingService(String path) throws Exception {
        mockMvc.perform(post(path).with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("  ")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(assistantService);
    }
}
