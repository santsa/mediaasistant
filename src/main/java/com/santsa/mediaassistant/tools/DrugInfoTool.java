package com.santsa.mediaassistant.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.santsa.mediaassistant.service.DrugInfoService;
import com.santsa.mediaassistant.dto.DrugInfo;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrugInfoTool {

    private final DrugInfoService drugInfoService;

    @Tool(description = "Search oficial information about medicine, purpose, advertisments, dosis and adverse events " +
            "use when the user ask about a specific drug")
    public DrugInfo getDrugInfo(@ToolParam(description = "Generic name of the drug in english, " +
            "for example: iuprofen, aspirin, metformin") String drugName) {

        log.info("Retrieving drug information for: {}", drugName);
        return drugInfoService.getDrugInfo(drugName);
    }
}
