package com.santsa.mediaassistant.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocumentIngestionService {

    private final VectorStore googleVectorStore;
    private final VectorStore ollamaVectorStore;

    @Value("${assistant.ingestion.ollama-enabled}")
    private boolean ollamaIngestionEnabled;

    @Value("${assistant.ingestion.gemini-enabled}")
    private boolean geminiIngestionEnabled;

    @Value("classpath:docs/pharmacology-iboprufen.pdf")
    private Resource iboprufen;

    @Value("classpath:docs/cardiovascular-care-protocol.pdf")
    private Resource cardiovascular;

    @Value("classpath:docs/hipertension-guide-medassistant.pdf")
    private Resource hipertension;

    public DocumentIngestionService(
            @Qualifier("googleVectorStore") VectorStore googleVectorStore,
            @Qualifier("ollamaVectorStore") VectorStore ollamaVectorStore) {
        this.googleVectorStore = googleVectorStore;
        this.ollamaVectorStore = ollamaVectorStore;
    }

    @PostConstruct
    public void ingest() {
        log.info("Init ingest medical documents");

        if (!geminiIngestionEnabled && !ollamaIngestionEnabled) {
            return;
        }

        List<Document> allDocuments = new ArrayList<>();
        allDocuments.addAll(readDocument(iboprufen));
        allDocuments.addAll(readDocument(cardiovascular));
        allDocuments.addAll(readDocument(hipertension));

        TokenTextSplitter splitter = TokenTextSplitter
                .builder()
                .withChunkSize(800)
                .withMaxNumChunks(1000)
                .build();

        List<Document> chunks = splitter.apply(allDocuments);
        if (geminiIngestionEnabled) {
            log.info("Init ingest medical documents google");
            googleVectorStore.add(chunks);
        } else {
            log.warn("Ingest medical documets disabled for gemini");
        }
        if (ollamaIngestionEnabled) {
            log.info("Init ingest medical documents ollama");
            ollamaVectorStore.add(chunks);
        } else {
            log.warn("Ingest medical documets disabled for ollama");
        }

        log.info("End ingest medical documents");
    }

    private List<Document> readDocument(Resource resource) {
        log.info("Reading document: {}", resource.getFilename());

        return new TikaDocumentReader(resource).get();
    }

}
