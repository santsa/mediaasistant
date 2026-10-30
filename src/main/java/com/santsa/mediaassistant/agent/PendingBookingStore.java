package com.santsa.mediaassistant.agent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.santsa.mediaassistant.dto.AppointmentSelection;

@Component
public class PendingBookingStore {

    private final Map<Long, AppointmentSelection> pending = new ConcurrentHashMap<>();

    public void store(Long userId, AppointmentSelection appointmentSelection) {
        pending.put(userId, appointmentSelection);
    }

    public AppointmentSelection retrieve(Long userId) {
        return pending.get(userId);
    }

    public AppointmentSelection remove(Long userId) {
        return pending.remove(userId);
    }
}
