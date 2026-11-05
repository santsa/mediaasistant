package com.santsa.mediaassistant.config;

import com.santsa.mediaassistant.tools.AppointmentBookingTool;
import com.santsa.mediaassistant.tools.AppointmentSearchTool;
import com.santsa.mediaassistant.tools.DoctorInfoTool;
import com.santsa.mediaassistant.tools.DrugInfoTool;
import com.santsa.mediaassistant.tools.PatientInfoTool;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("aws")
@RequiredArgsConstructor
public class AssistantConfigAws {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    private final AppointmentSearchTool appointmentSearchTool;
    private final DoctorInfoTool doctorInfoTool;
    private final PatientInfoTool patientInfoTool;
    private final DrugInfoTool drugInfoTool;
    private final AppointmentBookingTool appointmentBookingTool;
    private final MedicalAuditAdvisor medicalAuditAdvisor;

    @Bean("geminiClient")
    ChatClient geminiClient(GoogleGenAiChatModel chatModel,
            @Qualifier("googleVectorStore") ObjectProvider<VectorStore> vectorStoreProvider)
            throws IOException {

        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(
                        appointmentSearchTool, doctorInfoTool, patientInfoTool,
                        drugInfoTool, appointmentBookingTool)
                .defaultAdvisors(medicalAuditAdvisor);

        vectorStoreProvider.ifAvailable(vectorStore -> builder.defaultAdvisors(
                QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder()
                                .similarityThreshold(0.7)
                                .topK(3)
                                .build())
                        .promptTemplate(new PromptTemplate(
                                """
                                        Context of medical documents (use it if it is relevant to the question):
                                        {question_answer_context}
                                        If the information is not in the documents,
                                        you can use your tools or your general knowledge to answer."""))
                        .build()));

        return builder.build();
    }

    @Bean("googleVectorStore")
    VectorStore googleVectorStore(
            @Qualifier("googleGenAiTextEmbedding") EmbeddingModel embeddinModel,
            JdbcTemplate jdbcTemplate) {
        return PgVectorStore
                .builder(jdbcTemplate, embeddinModel)
                .dimensions(3072)
                .indexType(PgVectorStore.PgIndexType.NONE)
                .initializeSchema(true)
                .vectorTableName("medassistant_google")
                .build();
    }

}
