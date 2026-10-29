package com.santsa.mediaassistant.agent.orchestrator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.santsa.mediaassistant.config.ClientResolver;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class BibliographyWorker {

        private final ClientResolver clientResolver;

        @Value("classpath:prompts/orchestrator-worker-bibliography.st")
        private Resource bibliographyResource;

        private PromptTemplate bibliographyTemplate;

        @PostConstruct
        void init() {
                bibliographyTemplate = new PromptTemplate(bibliographyResource);
        }

        private String interpret(List<Document> documents, String symptoms, String model) {
                String documentContent = documents.stream()
                                .map(Document::getText)
                                .collect(Collectors.joining("\n\n---\n\n"));

                String prompt = bibliographyTemplate.render(Map.of(
                                "documents", documentContent,
                                "symptoms", symptoms));

                String result = ChatClient.create(clientResolver.resolveModel(model))
                                .prompt()
                                .system("You are a medical literature specialist.")
                                .user(prompt)
                                .call()
                                .content();

                log.info("Bibliography worker - completed analysis using {} documents", documents.size());
                return result;
        }

        private List<Document> searchdocuments(String symptoms, String model) {
                VectorStore vectorStore = clientResolver.resoleVectorStore(model);
                List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                                .query(symptoms)
                                .similarityThreshold(0.7)
                                .topK(3)
                                .build());

                log.info("Bibliography worker - found documents: {}", documents.size());
                return documents;

        }

        public String research(String symptoms, String model) {
                List<Document> documents = searchdocuments(symptoms, model);

                if (documents.isEmpty()) {
                        return "Not found relevant medical documents for tihs symptoms.";
                }
                return interpret(documents, symptoms, model);
        }

}
