package com.santsa.mediaassistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;

import com.santsa.mediaassistant.dto.IngestionResult;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceAwsImplTest {

    @Mock
    private VectorStore googleStore;

    private DocumentIngestionServiceAwsImpl service(boolean geminiEnabled) {
        return new DocumentIngestionServiceAwsImpl(googleStore, geminiEnabled,
                new ClassPathResource("docs/pharmacology-iboprufen.pdf"),
                new ClassPathResource("docs/cardiovascular-care-protocol.pdf"),
                new ClassPathResource("docs/hipertension-guide-medassistant.pdf"));
    }

    @Test
    void ingest_whenStoreEmpty_addsPdfChunks() {
        when(googleStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service(true).ingest();

        verify(googleStore).add(anyList());
    }

    @Test
    void ingest_whenStoreAlreadyHasDocuments_skipsAdding() {
        when(googleStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("existing")));

        service(true).ingest();

        verify(googleStore, never()).add(anyList());
    }

    @Test
    void ingest_whenGeminiIngestionDisabled_doesNothing() {
        service(false).ingest();

        verifyNoInteractions(googleStore);
    }

    @Test
    void ingestNewDocument_alwaysAddsAndReturnsFileNameAndChunkCount() {
        IngestionResult result = service(true).ingestNewDocument(
                DocumentIngestionServiceDefaultImplTest.textFile("notes.txt", "Hypertension is high blood pressure."));

        assertThat(result.fileName()).isEqualTo("notes.txt");
        assertThat(result.fragmentsCount()).isEqualTo(1);
        verify(googleStore).add(anyList());
        verify(googleStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void ingestNewDocument_whenGeminiIngestionDisabled_addsNothingAndReportsZeroFragments() {
        IngestionResult result = service(false).ingestNewDocument(
                DocumentIngestionServiceDefaultImplTest.textFile("notes.txt", "Hypertension is high blood pressure."));

        assertThat(result.fileName()).isEqualTo("notes.txt");
        assertThat(result.fragmentsCount()).isZero();
        verifyNoInteractions(googleStore);
    }
}
