package com.santsa.mediaassistant.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record QueryClassification(
    @JsonPropertyDescription("Type of the query, e.g., symptom report, general question, emergency, prescription request, or off-topic.")
    QueryType type,
    @JsonPropertyDescription("Explaination of the classification result, providing context or reasoning behind the classification.")
    String reason) {
}
