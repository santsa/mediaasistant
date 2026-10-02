package com.santsa.mediaassistant.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record PosibleCondition(
        @JsonPropertyDescription("Name of the medical condition") String name,
        @JsonPropertyDescription("Description of the medical condition") String description,
        @JsonPropertyDescription("Level of Severity of the medical condition") Severity severity) {

}
