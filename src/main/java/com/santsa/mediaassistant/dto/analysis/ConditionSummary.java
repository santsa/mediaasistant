package com.santsa.mediaassistant.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ConditionSummary(
                @JsonPropertyDescription("The name of the medical condition.") String conditionName,
                @JsonPropertyDescription("A brief description of the medical condition.") String description,
                @JsonPropertyDescription("Common symptoms associated with the medical condition.") String commonSymptoms,
                @JsonPropertyDescription("Potential treatments for the medical condition.") Severity severity,
                @JsonPropertyDescription("The urgency of the medical condition.") Urgency urgency,
                @JsonPropertyDescription("When to see a doctor for the medical condition.") String whenToSeeDoctor) {
}
