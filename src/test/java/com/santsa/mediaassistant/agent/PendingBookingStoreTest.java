package com.santsa.mediaassistant.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.santsa.mediaassistant.dto.AppointmentSelection;

class PendingBookingStoreTest {

    private final PendingBookingStore store = new PendingBookingStore();

    private static AppointmentSelection selection(String time) {
        return new AppointmentSelection("Cardiology", "2026-12-01", time, "Ana García", "Found a slot");
    }

    @Test
    void retrieve_whenNothingStored_returnsNull() {
        assertThat(store.retrieve(1L)).isNull();
    }

    @Test
    void store_thenRetrieve_returnsSelectionWithoutRemovingIt() {
        AppointmentSelection sel = selection("10:00");

        store.store(1L, sel);

        assertThat(store.retrieve(1L)).isSameAs(sel);
        assertThat(store.retrieve(1L)).isSameAs(sel);
    }

    @Test
    void store_whenCalledTwiceForSameUser_keepsLatestSelection() {
        AppointmentSelection latest = selection("11:00");

        store.store(1L, selection("10:00"));
        store.store(1L, latest);

        assertThat(store.retrieve(1L)).isSameAs(latest);
    }

    @Test
    void store_keepsSelectionsSeparatedByUser() {
        AppointmentSelection forUser1 = selection("10:00");
        AppointmentSelection forUser2 = selection("12:00");

        store.store(1L, forUser1);
        store.store(2L, forUser2);

        assertThat(store.retrieve(1L)).isSameAs(forUser1);
        assertThat(store.retrieve(2L)).isSameAs(forUser2);
    }

    @Test
    void remove_returnsStoredSelectionAndClearsIt() {
        AppointmentSelection sel = selection("10:00");
        store.store(1L, sel);

        AppointmentSelection removed = store.remove(1L);

        assertThat(removed).isSameAs(sel);
        assertThat(store.retrieve(1L)).isNull();
    }

    @Test
    void remove_whenNothingStored_returnsNull() {
        assertThat(store.remove(1L)).isNull();
    }
}
