package com.santsa.mediaassistant.service;

import com.santsa.mediaassistant.dto.analysis.ConditionSummary;

public interface AnaalysisService {
    ConditionSummary summarizeCondition(String condition, String model);
}
