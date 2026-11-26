package com.santsa.mediaassistant.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.santsa.mediaassistant.dto.DoctorInfo;
import com.santsa.mediaassistant.service.DoctorService;

@ExtendWith(MockitoExtension.class)
class DoctorInfoToolTest {

    @Mock
    private DoctorService doctorService;

    @InjectMocks
    private DoctorInfoTool tool;

    @Test
    void searchDoctors_delegatesQueryToServiceAndReturnsResult() {
        List<DoctorInfo> doctors = List.of(new DoctorInfo("Ana", "García", "Cardiology", "LIC-1", "600000000", "A-12"));
        when(doctorService.searchDoctors("García")).thenReturn(doctors);

        assertThat(tool.searchDoctors("García")).isSameAs(doctors);
    }
}
