package com.santsa.mediaassistant.service;

import java.util.List;

import com.santsa.mediaassistant.dto.analysis.ConditionSummary;
import com.santsa.mediaassistant.dto.analysis.QueryClassification;
import com.santsa.mediaassistant.dto.analysis.SymptomAnalysis;

public interface AnalysisService {
    ConditionSummary summarizeCondition(String condition, String model);
    List<ConditionSummary> listRelatedConditions(String symptoms, String model);
    SymptomAnalysis analyzeSymptoms(String symptoms, String model);
    QueryClassification classifyQuery(String query, String model);
}
