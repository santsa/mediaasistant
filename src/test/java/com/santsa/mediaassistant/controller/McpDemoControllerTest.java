package com.santsa.mediaassistant.controller;

import static com.santsa.mediaassistant.controller.ControllerTestSupport.body;
import static com.santsa.mediaassistant.controller.ControllerTestSupport.patient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.config.SecurityConfig;
import com.santsa.mediaassistant.support.FakeChatModel;

@WebMvcTest(McpDemoController.class)
@Import(SecurityConfig.class)
class McpDemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientResolver clientResolver;

    @MockitoBean
    private ToolCallbackProvider mcpTools;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void demo_sendsPromptToResolvedModelAndReturnsAnswer() throws Exception {
        FakeChatModel model = new FakeChatModel("mcp answer");
        when(clientResolver.resolveModel("gemini")).thenReturn(model);
        when(mcpTools.getToolCallbacks()).thenReturn(new ToolCallback[0]);

        mockMvc.perform(post("/api/v1/mcp/demo").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("list tools")))
                .andExpect(status().isOk())
                .andExpect(content().string("mcp answer"));

        assertThat(model.lastUserText()).isEqualTo("list tools");
    }

    @Test
    void demo_withBlankPrompt_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/mcp/demo").with(patient())
                .contentType(MediaType.APPLICATION_JSON).content(body("")))
                .andExpect(status().isBadRequest());
    }
}
