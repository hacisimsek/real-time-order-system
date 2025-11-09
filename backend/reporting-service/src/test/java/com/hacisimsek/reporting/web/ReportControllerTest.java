package com.hacisimsek.reporting.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hacisimsek.reporting.domain.ReportPeriod;
import com.hacisimsek.reporting.dto.ReportListResponse;
import com.hacisimsek.reporting.dto.ReportListResponse.PageMetadata;
import com.hacisimsek.reporting.dto.ReportSummaryResponse;
import com.hacisimsek.reporting.dto.ReportTotalsResponse;
import com.hacisimsek.reporting.dto.TopCustomerResponse;
import com.hacisimsek.reporting.service.ReportCsvExporter;
import com.hacisimsek.reporting.service.ReportService;
import com.hacisimsek.reporting.service.ReportSnapshotRefreshService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private ReportCsvExporter csvExporter;

    @MockBean
    private ReportSnapshotRefreshService refreshService;

    @Test
    void listOrderReportsReturnsData() throws Exception {
        LocalDate date = LocalDate.of(2025, 1, 3);
        List<ReportSummaryResponse> summaries = List.of(
                new ReportSummaryResponse(ReportPeriod.DAILY, date, 15, 12_345, new BigDecimal("123.45"))
        );
        ReportTotalsResponse totals = new ReportTotalsResponse(ReportPeriod.DAILY, null, null, 15, 12_345, new BigDecimal("123.45"));
        ReportListResponse response = new ReportListResponse(summaries,
                new PageMetadata(0, 20, 1, 1), totals);

        when(reportService.fetchSummaries(eq(ReportPeriod.DAILY), any(), any(), eq(0), eq(20), eq(false)))
                .thenReturn(response);

        mockMvc.perform(get("/reports/orders")
                        .param("period", "DAILY")
                        .param("refresh", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].period").value("DAILY"))
                .andExpect(jsonPath("$.items[0].snapshotDate").value("2025-01-03"))
                .andExpect(jsonPath("$.items[0].totalOrders").value(15))
                .andExpect(jsonPath("$.totals.totalRevenue").value(123.45));

        verify(refreshService).refresh(isNull(), isNull());
    }

    @Test
    void exportReturnsCsv() throws Exception {
        when(reportService.fetchSummaries(eq(ReportPeriod.WEEKLY), any(), any()))
                .thenReturn(List.of());
        when(csvExporter.toCsv(any())).thenReturn("period...".getBytes());

        mockMvc.perform(get("/reports/orders/export")
                        .param("period", "WEEKLY"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"));
    }

    @Test
    void refreshEndpointTriggersService() throws Exception {
        mockMvc.perform(post("/reports/orders/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2025-01-01\"}"))
                .andExpect(status().isAccepted());

        verify(refreshService).refresh(eq(LocalDate.of(2025, 1, 1)), isNull());
    }

    @Test
    void totalsEndpointReturnsAggregate() throws Exception {
        when(reportService.fetchTotals(eq(ReportPeriod.DAILY), any(), any()))
                .thenReturn(new ReportTotalsResponse(ReportPeriod.DAILY, null, null, 10, 1000, new BigDecimal("10.00")));

        mockMvc.perform(get("/reports/orders/totals")
                        .param("period", "DAILY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(10));

        verify(reportService).fetchTotals(eq(ReportPeriod.DAILY), any(), any());
    }

    @Test
    void topCustomersEndpointReturnsData() throws Exception {
        List<TopCustomerResponse> customers = List.of(
                new TopCustomerResponse("C-1", 10, 25_000, new BigDecimal("250.00"))
        );
        when(reportService.topCustomers(eq(ReportPeriod.DAILY), any(), any(), eq(3)))
                .thenReturn(customers);

        mockMvc.perform(get("/reports/orders/top-customers")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value("C-1"));

        verify(reportService).topCustomers(eq(ReportPeriod.DAILY), any(), any(), eq(3));
    }
}
