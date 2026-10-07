package com.santsa.mediaassistant.tools;

import java.util.List;

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
    public List<DoctorInfo> searchDoctors(
            @ToolParam(description = "Only the lastName of the doctor or specialty to search without title like Dr. or Dra.") String query) {
        log.info("Searching doctors with query: {}", query);
        return doctorService.searchDoctors(query);
    }
}
