package com.santsa.mediaassistant.mcp;

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

import io.modelcontextprotocol.spec.McpSchema;

@ExtendWith(MockitoExtension.class)
class DoctorResourceProviderTest {

    @Mock
    private DoctorService doctorService;

    @InjectMocks
    private DoctorResourceProvider provider;

    private static McpSchema.TextResourceContents onlyContent(McpSchema.ReadResourceResult result) {
        assertThat(result.contents()).hasSize(1);
        return (McpSchema.TextResourceContents) result.contents().getFirst();
    }

    @Test
    void getDoctorsBySpeciality_listsOneDoctorPerLineAsPlainText() {
        DoctorInfo ana = new DoctorInfo("Ana", "García", "Cardiology", "LIC-1", "600000000", "A-12");
        DoctorInfo luis = new DoctorInfo("Luis", "Pérez", "Cardiology", "LIC-2", "600000001", "A-13");
        when(doctorService.searchDoctors("cardiology")).thenReturn(List.of(ana, luis));

        McpSchema.TextResourceContents content = onlyContent(provider.getDoctorsBySpeciality("cardiology"));

        assertThat(content.uri()).isEqualTo("doctor://cardiology");
        assertThat(content.mimeType()).isEqualTo("text/plain");
        assertThat(content.text()).isEqualTo(ana + "\n" + luis);
    }

    @Test
    void getDoctorsBySpeciality_whenNoneFound_returnsNotFoundText() {
        when(doctorService.searchDoctors("dermatology")).thenReturn(List.of());

        McpSchema.TextResourceContents content = onlyContent(provider.getDoctorsBySpeciality("dermatology"));

        assertThat(content.text()).isEqualTo("Not found Doctors for this speciality");
    }
}
