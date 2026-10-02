package com.santsa.mediaassistant.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.santsa.mediaassistant.dto.analysis.ConditionSummary;
import com.santsa.mediaassistant.dto.ChatRequest;
import com.santsa.mediaassistant.service.AnaalysisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnaalysisService analysisService;

    @PostMapping("/condition")
    public ResponseEntity<ConditionSummary> analyzeCondition(
            @RequestBody ChatRequest chatRequest) {
        return ResponseEntity.ok(analysisService.summarizeCondition(chatRequest.prompt(), chatRequest.model()));
    }

}
