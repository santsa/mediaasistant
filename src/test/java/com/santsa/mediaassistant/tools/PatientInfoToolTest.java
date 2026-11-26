package com.santsa.mediaassistant.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import com.santsa.mediaassistant.dto.PatientInfo;
import com.santsa.mediaassistant.service.PatientService;

@ExtendWith(MockitoExtension.class)
class PatientInfoToolTest {

    @Mock
    private PatientService patientService;

    @InjectMocks
    private PatientInfoTool tool;

    private static ToolContext context(Long userId, String role) {
        return new ToolContext(Map.of("userId", userId, "role", role));
    }

    @Test
    void getPatientInfo_whenPatientRequestsOwnData_returnsPatientInfo() {
        PatientInfo info = new PatientInfo("Juan", "López", "1980-05-17", "Penicillin", "Hypertension");
        when(patientService.getPatientInfo(1L)).thenReturn(info);

        PatientInfo result = tool.getPatientInfo(1L, context(1L, "PATIENT"));

        assertThat(result).isSameAs(info);
    }

    @Test
    void getPatientInfo_whenPatientRequestsAnotherPatient_returnsNullAndDoesNotQuery() {
        PatientInfo result = tool.getPatientInfo(2L, context(1L, "PATIENT"));

        assertThat(result).isNull();
        verifyNoInteractions(patientService);
    }

    @Test
    void getPatientInfo_whenPatientIdIsNull_returnsNullAndDoesNotQuery() {
        PatientInfo result = tool.getPatientInfo(null, context(1L, "PATIENT"));

        assertThat(result).isNull();
        verifyNoInteractions(patientService);
    }

    @Test
    @DisplayName("By design ADMIN can book appointments but cannot read patient data")
    void getPatientInfo_whenAdminRequestsAnotherPatient_returnsNullBecauseOnlyOwnDataIsAllowed() {
        PatientInfo result = tool.getPatientInfo(2L, context(1L, "ADMIN"));

        assertThat(result).isNull();
        verifyNoInteractions(patientService);
    }

    @Test
    void getPatientInfo_whenUserIdMissingFromContext_returnsNullAndDoesNotQuery() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("role", "PATIENT");

        PatientInfo result = tool.getPatientInfo(1L, new ToolContext(ctx));

        assertThat(result).isNull();
        verifyNoInteractions(patientService);
    }
}
