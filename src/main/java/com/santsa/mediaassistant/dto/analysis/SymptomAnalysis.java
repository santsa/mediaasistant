package com.santsa.mediaassistant.dto.analysis;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record SymptomAnalysis(
        @JsonPropertyDescription("List of identified symptoms") List<String> identifyedSymptoms,
        @JsonPropertyDescription("List of possible conditions ordered by likelihood") List<PosibleCondition> possibleConditions,
        @JsonPropertyDescription("Level of urgency for the symptom") Urgency urgencyLevel,
        @JsonPropertyDescription("Recommendation for the patient, include if need medical attention") String recommendation) {
}
