package com.santsa.mediaassistant.agent;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.config.MedicalAuditAdvisor;
import com.santsa.mediaassistant.tools.AppointmentBookingTool;
import com.santsa.mediaassistant.tools.AppointmentSearchTool;
import com.santsa.mediaassistant.tools.DoctorInfoTool;
import com.santsa.mediaassistant.tools.DrugInfoTool;
import com.santsa.mediaassistant.tools.PatientInfoTool;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAgentServiceImpl implements OpenAgentService {

    private final ClientResolver clientResolver;
    private final AppointmentSearchTool appointmentSearchTool;
    private final DoctorInfoTool doctorInfoTool;
    private final PatientInfoTool patientInfoTool;
    private final DrugInfoTool drugInfoTool;

    @Value("classpath:prompts/open-agent.st")
    private Resource agentResource;

    private PromptTemplate agentTemplate;

    @PostConstruct
    void init() {
        agentTemplate = new PromptTemplate(agentResource);
    }

    @Override
    public String execute(String query, String model, Long userid) {
        log.info("Open Agent - query: {}, query");

        String systemPrompt = agentTemplate.render(Map.of(
                "currentDate", LocalDate.now().toString()));

        String result = ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system(systemPrompt)
                .user(query)
                .tools(appointmentSearchTool, doctorInfoTool, patientInfoTool, drugInfoTool)
                .toolContext(Map.of("userId", userid, "role", "PATIENT"))
                .call()
                .content();

        log.info("Open Agent - generated response");

        return result;
    }

}
