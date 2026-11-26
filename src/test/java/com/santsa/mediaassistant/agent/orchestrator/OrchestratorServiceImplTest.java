package com.santsa.mediaassistant.agent.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.config.MedicalAuditAdvisor;
import com.santsa.mediaassistant.support.FakeChatModel;

@ExtendWith(MockitoExtension.class)
class OrchestratorServiceImplTest {

    @Mock
    private ClientResolver clientResolver;
    @Mock
    private BibliographyWorker bibliographyWorker;
    @Mock
    private ClinicalWorker clinicalWorker;
    @Mock
    private HistoryWorker historyWorker;

    private FakeChatModel model;
    private OrchestratorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrchestratorServiceImpl(clientResolver, new MedicalAuditAdvisor(), bibliographyWorker,
                clinicalWorker, historyWorker);
        ReflectionTestUtils.setField(service, "synthesisResurce", new ClassPathResource("prompts/orchestrator-synthesis.st"));
        service.init();
        model = new FakeChatModel("final synthesis");
    }

    private void workersAnswer() {
        when(bibliographyWorker.research("fever", "gemini")).thenReturn("BIBLIO-RESULT");
        when(clinicalWorker.analyze("fever", "gemini")).thenReturn("CLINICAL-RESULT");
        when(historyWorker.lookup(1L)).thenReturn("HISTORY-RESULT");
        when(clientResolver.resolveModel("gemini")).thenReturn(model);
    }

    @Test
    void orchestrate_returnsSynthesisOfModel() {
        workersAnswer();

        assertThat(service.orchestrate("fever", "gemini", 1L)).isEqualTo("final synthesis");
    }

    @Test
    void orchestrate_passesAllThreeWorkerResultsAndSymptomsToSynthesisPrompt() {
        workersAnswer();

        service.orchestrate("fever", "gemini", 1L);

        assertThat(model.callCount()).isEqualTo(1);
        assertThat(model.lastUserText())
                .contains("BIBLIO-RESULT", "CLINICAL-RESULT", "HISTORY-RESULT", "fever");
        assertThat(model.lastSystemText()).contains("integrates information from multiple sources");
    }

    @Test
    void orchestrateWithProgress_emitsFourProgressStepsThenResult() {
        workersAnswer();

        List<String> events = service.orchestrateWithProgress("fever", "gemini", 1L).collectList().block();

        assertThat(events).hasSize(5);
        assertThat(events.subList(0, 4)).allMatch(e -> e.startsWith("[PROGRESS]"));
        assertThat(events.getLast()).isEqualTo("final synthesis");
    }

    @Test
    void orchestrateWithProgress_whenWorkerFails_emitsFirstProgressThenError() {
        when(bibliographyWorker.research("fever", "gemini")).thenThrow(new IllegalStateException("vector store down"));

        var flux = service.orchestrateWithProgress("fever", "gemini", 1L);

        assertThat(flux.take(1).collectList().block()).containsExactly("[PROGRESS] Searching medical bibliography...");
        assertThatThrownBy(() -> flux.collectList().block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("vector store down");
        verifyNoInteractions(clientResolver);
    }
}
