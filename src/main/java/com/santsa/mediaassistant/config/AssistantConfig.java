package com.santsa.mediaassistant.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import com.santsa.mediaassistant.tools.AppointmentBookingTool;
import com.santsa.mediaassistant.tools.AppointmentSearchTool;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;

import com.santsa.mediaassistant.tools.DoctorInfoTool;
import com.santsa.mediaassistant.tools.DrugInfoTool;
import com.santsa.mediaassistant.tools.PatientInfoTool;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

@Configuration
@RequiredArgsConstructor
public class AssistantConfig {

        @Value("classpath:prompts/system-prompt.st")
        private Resource systemPromptResource;

        private final AppointmentSearchTool appointmentSearchTool;
        private final DoctorInfoTool doctorInfoTool;
        private final PatientInfoTool patientInfoTool;
        private final DrugInfoTool drugInfoTool;
        private final AppointmentBookingTool appointmentBookingTool;
        private final ChatMemory chatMemory;

        @Bean("geminiClient")
        ChatClient geminiClient(GoogleGenAiChatModel chatModel,
                        @Qualifier("googleVectorStore") ObjectProvider<VectorStore> vectorStoreProvider)
                        throws IOException {
                return createChatClient(chatModel, vectorStoreProvider);
        }

        @Bean
        @Profile("ollama & !local")
        RestClientCustomizer ollamaBearerToken(@Value("${spring.ai.ollama.api-key}") String apiKey) {
                return builder -> builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        @Bean
        @Profile("ollama & !local")
        WebClientCustomizer ollamaWebClientBearerToken(@Value("${spring.ai.ollama.api-key}") String apiKey) {
                return builder -> builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        @Bean("ollamaClient")
        ChatClient ollamaClient(
                        OllamaChatModel chatModel,
                        @Qualifier("ollamaVectorStore") ObjectProvider<VectorStore> vectorStoreProvider)
                        throws IOException {
                return createChatClient(chatModel, vectorStoreProvider);
        }

        private ChatClient createChatClient(ChatModel chatModel,
                        ObjectProvider<VectorStore> vectorStoreProvider) throws IOException {
                String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                                .replace("{currentDate}", LocalDate.now().toString());

                ChatClient.Builder builder = ChatClient.builder(chatModel)
                                .defaultSystem(systemPrompt)
                                .defaultTools(
                                                appointmentSearchTool, doctorInfoTool, patientInfoTool,
                                                drugInfoTool, appointmentBookingTool)
                                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());

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
                        ChromaApi ChromaApi) {
                return ChromaVectorStore
                                .builder(ChromaApi, embeddinModel)
                                .collectionName("medassistant_google")
                                .initializeSchema(true)
                                .build();
        }

        @Bean("ollamaVectorStore")
        @Profile("!local")
        VectorStore remoteOllamaVectorStore(
                        @Qualifier("googleVectorStore") VectorStore googleVectorStore) {
                return googleVectorStore;
        }

        @Bean("ollamaVectorStore")
        @Profile("local")
        VectorStore ollamaVectorStore(
                        @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddinModel,
                        ChromaApi ChromaApi) {
                return ChromaVectorStore
                                .builder(ChromaApi, embeddinModel)
                                .collectionName("medassistant_ollama")
                                .initializeSchema(true)
                                .build();
        }

        @Bean
        ChromaApi chromaApi(@Value("${spring.ai.vectorstore.chroma.url}") String chromaUrl) {
                return ChromaApi.builder()
                                .baseUrl(chromaUrl)
                                .build();
        }
}
