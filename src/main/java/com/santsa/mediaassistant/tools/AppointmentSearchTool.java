package com.santsa.mediaassistant.tools;

import java.time.LocalDate;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.santsa.mediaassistant.dto.AppointmentInfo;
import com.santsa.mediaassistant.service.AppointmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentSearchTool {
    private final AppointmentService appointmentService;

    @Tool(description = "Search medical appointments available for a speciality and date. Using when user ask about medical appointments and medical slot availability.")
    public List<AppointmentInfo> searchAppointments(
            @ToolParam(description = "The medical speciality to search for example: cardiology, neurology, pediatrics") String speciality,
            @ToolParam(description = "The date to search for") String date) {
        log.info("Searching appointments for speciality: {} and date: {}", speciality, date);
        return appointmentService.findAvailableAppointments(speciality, LocalDate.parse(date));
    }

}
