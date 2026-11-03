package com.santsa.mediaassistant.tools;

import java.util.List;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.santsa.mediaassistant.dto.DoctorInfo;
import com.santsa.mediaassistant.service.DoctorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorInfoTool {
    private final DoctorService doctorService;

    @Tool(description = "Search for doctors based on a name or specialty. " +
            "Using when user ask about doctors and their information or who attends a speciality.")
    @McpTool(name = "search-doctors", description = "Search doctors for name and speciality")
    public List<DoctorInfo> searchDoctors(
            @ToolParam(description = "Only the lastName of the doctor or specialty " +
                    "to search without title like Dr. or Dra.") 
            @McpToolParam(description = "surname or medical speciality", required = true) String query) {
        log.info("Invoqued searchDoctors: Searching doctors with query: {}", query);
        return doctorService.searchDoctors(query);
    }
}
