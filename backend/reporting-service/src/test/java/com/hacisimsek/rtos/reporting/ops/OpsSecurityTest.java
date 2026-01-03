package com.hacisimsek.rtos.reporting.ops;

import com.hacisimsek.rtos.reporting.config.SecurityConfig;
import com.hacisimsek.rtos.security.JwtSecurityConfiguration;
import com.hacisimsek.rtos.security.JwtTokenIssuer;
import com.hacisimsek.rtos.security.Roles;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OpsController.class)
@Import({SecurityConfig.class, JwtSecurityConfiguration.class})
@TestPropertySource(properties = {
        "app.security.issuer=rtos",
        "app.security.audience=rtos-clients",
        "app.security.secrets[0]=unit-test-secret-which-is-long-enough-123456",
})
class OpsSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenIssuer issuer;

    @MockBean
    DlqReplayer replayer;

    @Test
    void replayRequiresExportRole() throws Exception {
        when(replayer.replay(anyString(), anyInt())).thenReturn(1);

        String readOnlyToken = issuer.issue(
                "tester@rtos.local",
                List.of(Roles.REPORTING_READ),
                Duration.ofMinutes(5)
        );

        mockMvc.perform(post("/ops/replay/dev.reporting.order-created.dlq")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + readOnlyToken))
                .andExpect(status().isForbidden());

        String exportToken = issuer.issue(
                "tester@rtos.local",
                List.of(Roles.REPORTING_EXPORT),
                Duration.ofMinutes(5)
        );

        mockMvc.perform(post("/ops/replay/dev.reporting.order-created.dlq")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + exportToken))
                .andExpect(status().isOk());
    }
}

