package com.santsa.mediaassistant.mcp;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import com.santsa.mediaassistant.dto.DoctorInfo;
import com.santsa.mediaassistant.service.DoctorService;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class DoctorResourceProvider {

    private final DoctorService doctorService;

    @McpResource(uri = "doctor://{speciality}", name = "Doctors for speciality", description = "List of available doctors for a medical speciality")
    public McpSchema.ReadResourceResult getDoctorsBySpeciality(String speciality) {
        List<DoctorInfo> doctorInfos = doctorService.searchDoctors(speciality);

        String content = doctorInfos.isEmpty()
                ? "Not found Doctors for this speciality"
                : listDoctors(doctorInfos);

        return McpSchema.ReadResourceResult.builder(
                List.of(
                        new McpSchema.TextResourceContents(
                                "doctor://" + speciality,
                                "text/plain", content, Map.of())))
                .build();
    }

    private String listDoctors(List<DoctorInfo> doctorInfos) {
        return doctorInfos.stream()
                .map(DoctorInfo::toString)
                .collect(Collectors.joining("\n"));
    }

}
