package com.santsa.mediaassistant.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.santsa.mediaassistant.dto.IngestionResult;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Profile("!aws")
@Slf4j
public class DocumentIngestionServiceDefaultImpl implements DocumentIngestionService{

    private static final int CHUNK_SIZE = 800;
    private static final int MAX_CHUNKS = 1000;

    private final VectorStore googleVectorStore;
    private final VectorStore ollamaVectorStore;
    private final boolean ollamaIngestionEnabled;
    private final boolean geminiIngestionEnabled;
    private final List<Resource> initialDocuments;

    public DocumentIngestionServiceDefaultImpl(
            @Qualifier("googleVectorStore") VectorStore googleVectorStore,
            @Qualifier("ollamaVectorStore") VectorStore ollamaVectorStore,
            @Value("${assistant.ingestion.ollama-enabled}") boolean ollamaIngestionEnabled,
            @Value("${assistant.ingestion.gemini-enabled}") boolean geminiIngestionEnabled,
            @Value("classpath:docs/pharmacology-iboprufen.pdf") Resource ibuprofen,
            @Value("classpath:docs/cardiovascular-care-protocol.pdf") Resource cardiovascular,
            @Value("classpath:docs/hipertension-guide-medassistant.pdf") Resource hypertension) {
        this.googleVectorStore = googleVectorStore;
        this.ollamaVectorStore = ollamaVectorStore;
        this.ollamaIngestionEnabled = ollamaIngestionEnabled;
        this.geminiIngestionEnabled = geminiIngestionEnabled;
        this.initialDocuments = List.of(ibuprofen, cardiovascular, hypertension);
    }

    @PostConstruct
    public void ingest() {
        if (!geminiIngestionEnabled && !ollamaIngestionEnabled) {
            log.info("Medical document ingestion is disabled for all providers");
            return;
        }

        ingestDocuments(readDocuments(initialDocuments));
    }

    private List<Document> readDocuments(List<Resource> resources) {
        return resources.stream()
                .flatMap(resource -> new TikaDocumentReader(resource).get().stream())
                .toList();
    }

    private List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(CHUNK_SIZE)
                .withMaxNumChunks(MAX_CHUNKS)
                .build();
        return splitter.apply(documents);
    }

    private List<Document> ingestDocuments(List<Document> documents) {
        log.info("Starting medical document ingestion");
        List<Document> chunks = splitDocuments(documents);
        ingestToStore(googleVectorStore, geminiIngestionEnabled, "Gemini", chunks);
        ingestToStore(ollamaVectorStore, ollamaIngestionEnabled, "Ollama", chunks);
        log.info("Finished medical document ingestion");
        return chunks;
    }

    private void ingestToStore(
            VectorStore vectorStore,
            boolean enabled,
            String provider,
            List<Document> chunks) {
        if (!enabled) {
            log.info("Medical document ingestion is disabled for {}", provider);
            return;
        }
        if (isIngested(vectorStore)) {
            log.info("Documents already exist in {} vector store; skipping ingestion", provider);
            return;
        }

        log.info("Ingesting medical documents into {} vector store", provider);
        vectorStore.add(chunks);
    }

    private boolean isIngested(VectorStore vectorStore) {
        return !vectorStore.similaritySearch(SearchRequest.builder()
                .query("medical document")
                .topK(1)
                .similarityThreshold(0.0)
                .build()).isEmpty();
    }

    @Override
    public IngestionResult ingestNewDocument(Resource resource) {
        List<Document> chunks = ingestDocuments(new TikaDocumentReader(resource).get());
        return new IngestionResult(resource.getFilename(), chunks.size());
    }
}
