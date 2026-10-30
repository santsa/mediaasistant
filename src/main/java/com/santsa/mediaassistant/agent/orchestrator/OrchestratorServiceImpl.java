package com.santsa.mediaassistant.agent.orchestrator;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.config.MedicalAuditAdvisor;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrchestratorServiceImpl implements OrchestratorService {

        private final ClientResolver clientResolver;
        private final MedicalAuditAdvisor medicalAuditAdvisor;
        private final BibliographyWorker bibliographyWorker;
        private final ClinicalWorker clinicalWorker;
        private final HistoryWorker historyWorker;

        @Value("classpath:prompts/orchestrator-synthesis.st")
        private Resource synthesisResurce;

        private PromptTemplate synthesisTemplate;

        @PostConstruct
        void init() {
                synthesisTemplate = new PromptTemplate(synthesisResurce);
        }

        public String orchestrate(String symptoms, String model, Long userId) {
                log.info("Orchestrator - init integral analisi");

                String bibliography = bibliographyWorker.research(symptoms, model);
                String clinicalAnalysis = clinicalWorker.analyze(symptoms, model);
                String historical = historyWorker.lookup(userId);

                log.info("Orchestrator - three workers completed their work");
                return synthesize(clinicalAnalysis, historical,
                                bibliography, symptoms, model, userId);
        }

        private String synthesize(String clinicalAnalysis, String historical,
                        String bibliography, String symptoms, String model, Long userId) {

                String result = ChatClient.create(clientResolver.resolveModel(model))
                                .prompt()
                                .system("You are adoctor who integrates information from multiple sources to provide a comprehensive answer.")
                                .user(synthesisTemplate.render(Map.of(
                                                "clinicalAnalysis", clinicalAnalysis,
                                                "historical", historical,
                                                "bibliography", bibliography,
                                                "symptoms", symptoms)))
                                .advisors(a -> a.advisors(medicalAuditAdvisor)
                                                .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                                .call()
                                .content();

                log.info("Orchestrator - completed synthesis");
                return result;

        }

        @Override
        public Flux<String> orchestrateWithProgress(String symptoms, String model, Long userId) {

                return Flux.<String>create(sink -> {
                        try {
                                sink.next("[PROGRESS] Searching medical bibliography...");
                                String bibliography = bibliographyWorker.research(symptoms, model);

                                sink.next("[PROGRESS] Analyzing symptoms clinicaly...");
                                String clinicalAnalysis = clinicalWorker.analyze(symptoms, model);

                                sink.next("[PROGRESS] Consulting patient history....");
                                String historical = historyWorker.lookup(userId);

                                sink.next("[PROGRESS] Integrating results of tree speacialists...");
                                String result = synthesize(clinicalAnalysis, historical,
                                                bibliography, symptoms, model, userId);

                                sink.next(result);
                                sink.complete();
                        } catch (Exception e) {
                                log.error("Orquestrator (progress) - error: {}", e.getMessage());
                                sink.error(e);
                        }
                }).subscribeOn(Schedulers.boundedElastic());
        }

}
