package com.santsa.mediaassistant.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.santsa.mediaassistant.dto.DrugInfo;
import com.santsa.mediaassistant.service.DrugInfoService;

@ExtendWith(MockitoExtension.class)
class DrugInfoToolTest {

    @Mock
    private DrugInfoService drugInfoService;

    @InjectMocks
    private DrugInfoTool tool;

    @Test
    void getDrugInfo_delegatesToServiceAndReturnsResult() {
        DrugInfo info = new DrugInfo("Advil", "ibuprofen", "Pain reliever", "Stomach bleeding", "1 tablet every 6h");
        when(drugInfoService.getDrugInfo("ibuprofen")).thenReturn(info);

        assertThat(tool.getDrugInfo("ibuprofen")).isSameAs(info);
    }

    @Test
    void getDrugInfo_whenServiceFindsNothing_returnsNull() {
        when(drugInfoService.getDrugInfo("unknown")).thenReturn(null);

        assertThat(tool.getDrugInfo("unknown")).isNull();
    }
}
