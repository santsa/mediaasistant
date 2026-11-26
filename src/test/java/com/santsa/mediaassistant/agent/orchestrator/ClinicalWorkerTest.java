package com.santsa.mediaassistant.agent.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.support.FakeChatModel;

@ExtendWith(MockitoExtension.class)
class ClinicalWorkerTest {

    @Mock
    private ClientResolver clientResolver;

    @Test
    void analyze_rendersClinicalTemplateWithSymptomsAndReturnsModelContent() {
        ClinicalWorker worker = new ClinicalWorker(clientResolver);
        ReflectionTestUtils.setField(worker, "clinicalResource",
                new ClassPathResource("prompts/orchestrator-worker-clinical.st"));
        worker.init();
        FakeChatModel model = new FakeChatModel("clinical analysis");
        when(clientResolver.resolveModel("ollama")).thenReturn(model);

        String result = worker.analyze("dizziness", "ollama");

        assertThat(result).isEqualTo("clinical analysis");
        assertThat(model.lastUserText()).startsWith("You are a clinical reasoning specialist").contains("dizziness");
        assertThat(model.lastSystemText()).isEqualTo("You are an spacialyst in clinical reasoning.");
    }
}
