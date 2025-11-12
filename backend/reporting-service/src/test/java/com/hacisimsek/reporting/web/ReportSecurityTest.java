package com.hacisimsek.reporting.web;

import com.hacisimsek.reporting.domain.ReportPeriod;
import com.hacisimsek.reporting.dto.ReportListResponse;
import com.hacisimsek.reporting.dto.ReportListResponse.PageMetadata;
import com.hacisimsek.reporting.dto.ReportSummaryResponse;
import com.hacisimsek.reporting.dto.ReportTotalsResponse;
import com.hacisimsek.reporting.service.ReportCsvExporter;
import com.hacisimsek.reporting.service.ReportService;
import com.hacisimsek.reporting.service.ReportSnapshotRefreshService;
import com.hacisimsek.reporting.config.SecurityConfig;
import com.hacisimsek.security.JwtSecurityConfiguration;
import com.hacisimsek.security.JwtTokenIssuer;
import com.hacisimsek.security.Roles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class)
@Import({SecurityConfig.class, JwtSecurityConfiguration.class})
@TestPropertySource(properties = {
        "app.security.issuer=rtos",
        "app.security.audience=rtos-clients",
        "app.security.secrets[0]=unit-test-secret-which-is-long-enough-123456",
})
class ReportSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenIssuer issuer;

    @MockBean
    ReportService reportService;

    @MockBean
    ReportCsvExporter csvExporter;

    @MockBean
    ReportSnapshotRefreshService refreshService;

    @Test
    void authorizedTokenCanAccessReports() throws Exception {
        ReportSummaryResponse summary = new ReportSummaryResponse(
                ReportPeriod.DAILY,
                LocalDate.of(2025, 1, 3),
                10,
                25_000,
                new BigDecimal("250.00")
        );
        ReportTotalsResponse totals = new ReportTotalsResponse(
                ReportPeriod.DAILY,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 3),
                10,
                25_000,
                new BigDecimal("250.00")
        );
        ReportListResponse response = new ReportListResponse(
                List.of(summary),
                new PageMetadata(0, 20, 1, 1),
                totals
        );

        when(reportService.fetchSummaries(eq(ReportPeriod.DAILY), any(), any(), eq(0), eq(20), eq(false)))
                .thenReturn(response);

        String token = issuer.issue(
                "reporter@rtos.local",
                List.of(Roles.REPORTING_READ),
                Duration.ofMinutes(5)
        );

        mockMvc.perform(get("/reports/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("period", "DAILY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
