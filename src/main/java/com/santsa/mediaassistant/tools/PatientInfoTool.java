package com.santsa.mediaassistant.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.santsa.mediaassistant.dto.PatientInfo;
import com.santsa.mediaassistant.service.PatientService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PatientInfoTool {
    private final PatientService patientService;

    @Tool(description = "Fetch patient information based on patient ID. " +
            "Use this tool when the user asks for patient allergy, condition or medical history.")
    public PatientInfo getPatientInfo(
            @ToolParam(description = "The unique identifier of the patient whose information is to be retrieved.") Long patientId,
            ToolContext context) {
        log.info("Fetching patient info for patientId: {}", patientId);

        Long userId = (Long) context.getContext().get("userId");
        log.info("ToolContext User ID: {}", userId);

        if (!patientId.equals(userId)) {
            log.warn("Unauthorized access attempt by userId: {} for patientId: {}", userId, patientId);
            return null; // or throw an exception, or return an error message
        }

        return patientService.getPatientInfo(patientId);
    }

}
