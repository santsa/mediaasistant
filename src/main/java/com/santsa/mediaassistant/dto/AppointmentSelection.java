package com.santsa.mediaassistant.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AppointmentSelection(
                @JsonPropertyDescription("Medical speciality of selected appointment") String specialty,

                @JsonPropertyDescription("Data selected appointment in format yyyy-MM-dd") String date,

                @JsonPropertyDescription("Hour selected appointment in format  HH:mm") String time,

                @JsonPropertyDescription("Name of the doctor selected appointment") String doctorName,

                @JsonPropertyDescription("Brief message inform to the patient about founded appointment") String message) {
}
