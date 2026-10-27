package com.santsa.mediaassistant.tools;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.santsa.mediaassistant.service.AppointmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentBookingTool {

    private final AppointmentService appointmentService;

    @Tool(description = "Book a medical turn to a patient. " +
            "Only administrative personal can execute this action. " +
            "Ask confirmation before book.")
    public String bookAppointment(
            @ToolParam(description = "Medical speciality") String speciality,
            @ToolParam(description = "Date of appointment in format yyy-MM-dd") String date,
            @ToolParam(description = "Hour of the appointment in format HH:mm") String time,
            @ToolParam(description = "The unique identifier of the patient") Long patientId,
            ToolContext context // only admin
    ) {
        log.info("Invoqued Searching bookAppointment: for speciality: {}, date: {}, time: {} and patientId: {}",
                speciality, date, time, patientId);

        String role = context.getContext().get("role").toString();
        if (!"ADMIN".equals(role)) {
            log.warn("Denied access: rol {} try to book appointment", role);
            return "Denied access. Only admin can book appointments";
        }
        return appointmentService.bookAppointment(speciality, LocalDate.parse(date), LocalTime.parse(time), patientId);
    }
}
