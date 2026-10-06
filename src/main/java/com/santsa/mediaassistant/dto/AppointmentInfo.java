package com.santsa.mediaassistant.dto;

public record AppointmentInfo(
    String doctorName,
    String specialty,
    String date,
    String time
) {

}
