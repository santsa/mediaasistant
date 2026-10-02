package com.santsa.mediaassistant.dto.analysis;

public record ConditionSummary(
        String conditionName,
        String description,
        String commonSymptoms,
        String whenToSeeDoctor) {
}
