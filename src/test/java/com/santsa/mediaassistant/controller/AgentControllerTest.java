package com.santsa.mediaassistant.controller;

import static com.santsa.mediaassistant.controller.ControllerTestSupport.USER_ID;
import static com.santsa.mediaassistant.controller.ControllerTestSupport.body;
import static com.santsa.mediaassistant.controller.ControllerTestSupport.patient;
import static com.santsa.mediaassistant.controller.ControllerTestSupport.userWithoutRole;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.santsa.mediaassistant.agent.AppointmentChainService;
import com.santsa.mediaassistant.agent.OpenAgentService;
import com.santsa.mediaassistant.agent.RoutingWorkflowService;
import com.santsa.mediaassistant.agent.orchestrator.OrchestratorService;
import com.santsa.mediaassistant.config.SecurityConfig;

import reactor.core.publisher.Flux;

@WebMvcTest(AgentController.class)
@Import(SecurityConfig.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentChainService appointmentChainService;
    @MockitoBean
    private RoutingWorkflowService routingWorkflowService;
    @MockitoBean
    private OrchestratorService orchestratorService;
    @MockitoBean
    private OpenAgentService openAgentService;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void routing_withoutPatientOrAdminRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/agent/routing").with(userWithoutRole())
                .contentType(MediaType.APPLICATION_JSON).content(body("hi")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(routingWorkflowService);
    }

    @Test
    void routing_delegatesWithUserIdFromJwt() throws Exception {
        when(routingWorkflowService.routeQuery("headache", "gemini", USER_ID)).thenReturn("routed");

        mockMvc.perform(post("/api/v1/agent/routing").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("headache")))
                .andExpect(status().isOk())
                .andExpect(content().string("routed"));
    }

    @Test
    void routing_withBlankPrompt_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/agent/routing").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routingWorkflowService);
    }

    @Test
    void chainAppointment_delegatesToBookAppointmentChain() throws Exception {
        when(appointmentChainService.bookAppointmentChain("cardio tomorrow", "gemini", USER_ID)).thenReturn("proposal");

        mockMvc.perform(post("/api/v1/agent/chain/appointment").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("cardio tomorrow")))
                .andExpect(status().isOk())
                .andExpect(content().string("proposal"));
    }

    @Test
    void confirm_needsNoBodyAndUsesUserIdFromJwt() throws Exception {
        when(appointmentChainService.confirmBooking(USER_ID)).thenReturn("Reserved turn succesfully");

        mockMvc.perform(post("/api/v1/agent/chain/appointment/confirm").with(patient()))
                .andExpect(status().isOk())
                .andExpect(content().string("Reserved turn succesfully"));
    }

    @Test
    void cancel_needsNoBodyAndUsesUserIdFromJwt() throws Exception {
        when(appointmentChainService.cancelBooking(USER_ID)).thenReturn("Appointment canceled.");

        mockMvc.perform(post("/api/v1/agent/chain/appointment/cancel").with(patient()))
                .andExpect(status().isOk())
                .andExpect(content().string("Appointment canceled."));
    }

    @Test
    void orchestrator_delegatesToOrchestrate() throws Exception {
        when(orchestratorService.orchestrate("fever", "gemini", USER_ID)).thenReturn("synthesis");

        mockMvc.perform(post("/api/v1/agent/orchestrator").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("fever")))
                .andExpect(status().isOk())
                .andExpect(content().string("synthesis"));
    }

    @Test
    void orchestratorProgress_streamsEventsAsSse() throws Exception {
        when(orchestratorService.orchestrateWithProgress("fever", "gemini", USER_ID))
                .thenReturn(Flux.just("[PROGRESS] step", "final"));

        MvcResult result = mockMvc.perform(post("/api/v1/agent/orchestrator/progress").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("fever")))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("data:[PROGRESS] step")))
                .andExpect(content().string(containsString("data:final")));
    }

    @Test
    void open_delegatesToOpenAgent() throws Exception {
        when(openAgentService.execute("find a doctor", "gemini", USER_ID)).thenReturn("agent result");

        mockMvc.perform(post("/api/v1/agent/open").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("find a doctor")))
                .andExpect(status().isOk())
                .andExpect(content().string("agent result"));
    }
}
