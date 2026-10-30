package com.santsa.mediaassistant.dto.agent;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AppointmentRequest(
        @JsonPropertyDescription("Medical speciality requested, for example: cardiology, pediatry, dermatology") String specialty,
        @JsonPropertyDescription("Date requested in format yyy-MM-dd") String date) {

}
