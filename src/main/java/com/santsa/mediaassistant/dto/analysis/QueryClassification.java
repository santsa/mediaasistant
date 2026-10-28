package com.santsa.mediaassistant.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record QueryClassification(
        @JsonPropertyDescription("Type of query identified.") QueryType type,
        @JsonPropertyDescription("Briefy explanation about it is this classification.") String reason) {
}
