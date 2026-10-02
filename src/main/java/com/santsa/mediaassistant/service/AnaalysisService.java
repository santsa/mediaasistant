package com.santsa.mediaassistant.service;

import java.util.List;

import com.santsa.mediaassistant.dto.analysis.ConditionSummary;

public interface AnaalysisService {
    ConditionSummary summarizeCondition(String condition, String model);

    List<ConditionSummary> listRelatedConditions(String symptoms, String model);
}
