package com.santsa.mediaassistant.dto;

public record DoctorInfo(
        String firstName,
        String lastName,
        String speciality,
        String licenseNumber,
        String phone,
        String office) {

}
