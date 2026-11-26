package com.santsa.mediaassistant.controller;

import static com.santsa.mediaassistant.controller.ControllerTestSupport.admin;
import static com.santsa.mediaassistant.controller.ControllerTestSupport.patient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.santsa.mediaassistant.config.SecurityConfig;
import com.santsa.mediaassistant.dto.IngestionResult;
import com.santsa.mediaassistant.service.DocumentIngestionService;

@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
class DocumentControllerTest {

    private static final MockMultipartFile FILE =
            new MockMultipartFile("file", "guide.pdf", "application/pdf", "fake pdf".getBytes());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentIngestionService documentIngestionService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void ingest_withoutToken_returns401() throws Exception {
        mockMvc.perform(multipart("/api/v1/documents/ingest").file(FILE))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentIngestionService);
    }

    @Test
    void ingest_asPatient_returns403FromPreAuthorize() throws Exception {
        mockMvc.perform(multipart("/api/v1/documents/ingest").file(FILE).with(patient()))
                .andExpect(status().isForbidden())
                .andExpect(content().string("You don't have permission for this acction"));

        verifyNoInteractions(documentIngestionService);
    }

    @Test
    void ingest_asAdmin_passesUploadedFileAndReturnsResultAsJson() throws Exception {
        when(documentIngestionService.ingestNewDocument(any())).thenReturn(new IngestionResult("guide.pdf", 12));

        mockMvc.perform(multipart("/api/v1/documents/ingest").file(FILE).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("guide.pdf"))
                .andExpect(jsonPath("$.fragmentsCount").value(12));

        ArgumentCaptor<Resource> resource = ArgumentCaptor.forClass(Resource.class);
        verify(documentIngestionService).ingestNewDocument(resource.capture());
        assertThat(resource.getValue().getFilename()).isEqualTo("guide.pdf");
        assertThat(resource.getValue().getContentAsByteArray()).isEqualTo("fake pdf".getBytes());
    }

    @Test
    void ingest_withoutFilePart_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/documents/ingest").with(admin()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentIngestionService);
    }
}
