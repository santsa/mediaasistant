package com.santsa.mediaassistant.dto;

public record PatientInfo(
        String firstName,
        String lastName,
        String dateOfBirth,
        String allergies,
        String conditions) {

}
