package com.santsa.mediaassistant.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import com.santsa.mediaassistant.config.ClientResolver;
import com.santsa.mediaassistant.service.AppointmentService;
import com.santsa.mediaassistant.service.DoctorService;
import com.santsa.mediaassistant.service.DrugInfoService;
import com.santsa.mediaassistant.service.PatientService;
import com.santsa.mediaassistant.support.FakeChatModel;
import com.santsa.mediaassistant.tools.AppointmentSearchTool;
import com.santsa.mediaassistant.tools.DoctorInfoTool;
import com.santsa.mediaassistant.tools.DrugInfoTool;
import com.santsa.mediaassistant.tools.PatientInfoTool;

@ExtendWith(MockitoExtension.class)
class OpenAgentServiceImplTest {

    @Mock
    private ClientResolver clientResolver;

    private FakeChatModel model;
    private OpenAgentServiceImpl service;

    @BeforeEach
    void setUp() {
        // Real tool instances (backed by mocked services) so their @Tool metadata is registered.
        service = new OpenAgentServiceImpl(clientResolver,
                new AppointmentSearchTool(mock(AppointmentService.class)),
                new DoctorInfoTool(mock(DoctorService.class)),
                new PatientInfoTool(mock(PatientService.class)),
                new DrugInfoTool(mock(DrugInfoService.class)));
        ReflectionTestUtils.setField(service, "agentResource", new ClassPathResource("prompts/open-agent.st"));
        service.init();
        model = new FakeChatModel("agent answer");
        when(clientResolver.resolveModel("gemini")).thenReturn(model);
    }

    @Test
    void execute_returnsModelContentForUserQuery() {
        String result = service.execute("Find me a cardiologist", "gemini", 1L);

        assertThat(result).isEqualTo("agent answer");
        assertThat(model.lastUserText()).isEqualTo("Find me a cardiologist");
    }

    @Test
    void execute_rendersSystemPromptWithCurrentDate() {
        service.execute("hello", "gemini", 1L);

        assertThat(model.lastSystemText())
                .startsWith("You are an autonomous medical agent")
                .contains(LocalDate.now().toString());
    }

    @Test
    void execute_exposesTheFourReadOnlyToolsButNotBooking() {
        service.execute("hello", "gemini", 1L);

        assertThat(model.lastToolNames())
                .containsExactlyInAnyOrder("searchAppointments", "searchDoctors", "getPatientInfo", "getDrugInfo")
                .doesNotContain("bookAppointment");
    }

    @Test
    void execute_alwaysRunsToolsWithPatientRoleAndCallerUserId() {
        service.execute("hello", "gemini", 42L);

        assertThat(model.lastToolContext()).containsEntry("userId", 42L).containsEntry("role", "PATIENT");
    }
}
