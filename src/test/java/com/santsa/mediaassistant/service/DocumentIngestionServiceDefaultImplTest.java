package com.santsa.mediaassistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.santsa.mediaassistant.dto.IngestionResult;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceDefaultImplTest {

    @Mock
    private VectorStore googleStore;

    @Mock
    private VectorStore ollamaStore;

    private DocumentIngestionServiceDefaultImpl service(boolean ollamaEnabled, boolean geminiEnabled) {
        return new DocumentIngestionServiceDefaultImpl(googleStore, ollamaStore, ollamaEnabled, geminiEnabled,
                new ClassPathResource("docs/pharmacology-iboprufen.pdf"),
                new ClassPathResource("docs/cardiovascular-care-protocol.pdf"),
                new ClassPathResource("docs/hipertension-guide-medassistant.pdf"));
    }

    static Resource textFile(String name, String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return name;
            }
        };
    }

    @Test
    void ingest_whenBothProvidersDisabled_doesNothing() {
        service(false, false).ingest();

        verifyNoInteractions(googleStore, ollamaStore);
    }

    @Test
    @SuppressWarnings("unchecked")
    void ingest_whenGeminiEnabledAndStoreEmpty_addsPdfChunksOnlyToGoogleStore() {
        when(googleStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        ArgumentCaptor<List<Document>> chunks = ArgumentCaptor.forClass(List.class);

        service(false, true).ingest();

        verify(googleStore).add(chunks.capture());
        assertThat(chunks.getValue()).isNotEmpty();
        verifyNoInteractions(ollamaStore);
    }

    @Test
    void ingest_whenStoreAlreadyHasDocuments_skipsAdding() {
        when(googleStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("existing")));
        when(ollamaStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("existing")));

        service(true, true).ingest();

        verify(googleStore, never()).add(anyList());
        verify(ollamaStore, never()).add(anyList());
    }

    @Test
    void ingest_checksExistingContentWithTopK1AndThreshold0() {
        when(googleStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("existing")));
        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

        service(false, true).ingest();

        verify(googleStore).similaritySearch(request.capture());
        assertThat(request.getValue().getTopK()).isEqualTo(1);
        assertThat(request.getValue().getSimilarityThreshold()).isZero();
    }

    @Test
    void ingestNewDocument_addsChunksToEnabledStoresAndReturnsFileNameAndCount() {
        IngestionResult result = service(true, true)
                .ingestNewDocument(textFile("notes.txt", "Hypertension is high blood pressure."));

        assertThat(result.fileName()).isEqualTo("notes.txt");
        assertThat(result.fragmentsCount()).isEqualTo(1);
        verify(googleStore).add(anyList());
        verify(ollamaStore).add(anyList());
    }

    @Test
    void ingestNewDocument_alwaysAddsEvenIfStoreAlreadyHasDocuments() {
        // Regression: uploads used to be skipped because the store is never empty after startup.
        IngestionResult result = service(false, true)
                .ingestNewDocument(textFile("notes.txt", "Hypertension is high blood pressure."));

        assertThat(result.fragmentsCount()).isEqualTo(1);
        verify(googleStore).add(anyList());
        verify(googleStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void ingestNewDocument_doesNotAddToDisabledProvider() {
        service(false, true).ingestNewDocument(textFile("notes.txt", "Hypertension is high blood pressure."));

        verifyNoInteractions(ollamaStore);
    }
}
