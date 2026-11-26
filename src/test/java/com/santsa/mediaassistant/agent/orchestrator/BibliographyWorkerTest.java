package com.santsa.mediaassistant.agent.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.support.FakeChatModel;

@ExtendWith(MockitoExtension.class)
class BibliographyWorkerTest {

    @Mock
    private ClientResolver clientResolver;

    @Mock
    private VectorStore vectorStore;

    private BibliographyWorker worker;

    @BeforeEach
    void setUp() {
        worker = new BibliographyWorker(clientResolver);
        ReflectionTestUtils.setField(worker, "bibliographyResource",
                new ClassPathResource("prompts/orchestrator-worker-bibliography.st"));
        worker.init();
        when(clientResolver.resolveVectorStore("gemini")).thenReturn(vectorStore);
    }

    @Test
    void research_searchesVectorStoreWithSymptomsTopK3AndThreshold07() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

        worker.research("hypertension", "gemini");

        verify(vectorStore).similaritySearch(request.capture());
        assertThat(request.getValue().getQuery()).isEqualTo("hypertension");
        assertThat(request.getValue().getTopK()).isEqualTo(3);
        assertThat(request.getValue().getSimilarityThreshold()).isEqualTo(0.7);
    }

    @Test
    void research_whenNoDocumentsFound_returnsMessageWithoutCallingModel() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        String result = worker.research("hypertension", "gemini");

        assertThat(result).isEqualTo("Not found relevant medical documents for tihs symptoms.");
        verify(clientResolver, never()).resolveModel(any());
    }

    @Test
    void research_whenDocumentsFound_sendsTheirTextJoinedWithSymptomsToModel() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("DOC-ONE"), new Document("DOC-TWO")));
        FakeChatModel model = new FakeChatModel("literature summary");
        when(clientResolver.resolveModel("gemini")).thenReturn(model);

        String result = worker.research("hypertension", "gemini");

        assertThat(result).isEqualTo("literature summary");
        assertThat(model.lastUserText()).contains("DOC-ONE\n\n---\n\nDOC-TWO").contains("hypertension");
        assertThat(model.lastSystemText()).isEqualTo("You are a medical literature specialist.");
    }
}
