package com.hacisimsek.rtos.reporting.web;

import com.hacisimsek.rtos.reporting.domain.ReportPeriod;
import com.hacisimsek.rtos.reporting.dto.ReportRefreshRequest;
import com.hacisimsek.rtos.reporting.dto.ReportListResponse;
import com.hacisimsek.rtos.reporting.dto.ReportSummaryResponse;
import com.hacisimsek.rtos.reporting.dto.ReportTotalsResponse;
import com.hacisimsek.rtos.reporting.dto.TopCustomerResponse;
import com.hacisimsek.rtos.reporting.service.ReportCsvExporter;
import com.hacisimsek.rtos.reporting.service.ReportService;
import com.hacisimsek.rtos.reporting.service.ReportSnapshotRefreshService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports/orders")
@Validated
public class ReportController {

    private final ReportService reportService;
    private final ReportCsvExporter csvExporter;
    private final ReportSnapshotRefreshService refreshService;

    public ReportController(ReportService reportService,
                            ReportCsvExporter csvExporter,
                            ReportSnapshotRefreshService refreshService) {
        this.reportService = reportService;
        this.csvExporter = csvExporter;
        this.refreshService = refreshService;
    }

    @GetMapping
    @PreAuthorize("""
            hasAnyAuthority(
                T(com.hacisimsek.rtos.security.Roles).REPORTING_READ,
                T(com.hacisimsek.rtos.security.Roles).REPORTING_EXPORT)
            """)
    public ReportListResponse listOrderReports(
            @RequestParam(name = "period", defaultValue = "DAILY") ReportPeriod period,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "asc") String sort
    ) {
        if (refresh) {
            refreshService.refresh(startDate, endDate);
        }
        boolean descending = "desc".equalsIgnoreCase(sort);
        return reportService.fetchSummaries(period, startDate, endDate, page, size, descending);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAuthority(T(com.hacisimsek.rtos.security.Roles).REPORTING_EXPORT)")
    public ResponseEntity<byte[]> exportOrderReports(
            @RequestParam(name = "period", defaultValue = "DAILY") ReportPeriod period,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<ReportSummaryResponse> summaries = reportService.fetchSummaries(period, startDate, endDate);
        byte[] payload = csvExporter.toCsv(summaries);
        String filename = "order-reports-%s-%s.csv".formatted(
                period.name().toLowerCase(),
                LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(payload);
    }

    @PreAuthorize("hasAuthority(T(com.hacisimsek.rtos.security.Roles).REPORTING_EXPORT)")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshSnapshots(@RequestBody(required = false)
                                              ReportRefreshRequest request) {
        LocalDate start = request != null ? request.startDate() : null;
        LocalDate end = request != null ? request.endDate() : null;
        refreshService.refresh(start, end);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/totals")
    @PreAuthorize("hasAnyAuthority(T(com.hacisimsek.rtos.security.Roles).REPORTING_READ, T(com.hacisimsek.rtos.security.Roles).REPORTING_EXPORT)")
    public ReportTotalsResponse totals(
            @RequestParam(name = "period", defaultValue = "DAILY") ReportPeriod period,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return reportService.fetchTotals(period, startDate, endDate);
    }

    @GetMapping("/top-customers")
    @PreAuthorize("hasAnyAuthority(T(com.hacisimsek.rtos.security.Roles).REPORTING_READ, T(com.hacisimsek.rtos.security.Roles).REPORTING_EXPORT)")
    public List<TopCustomerResponse> topCustomers(
            @RequestParam(name = "period", defaultValue = "DAILY") ReportPeriod period,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "limit", defaultValue = "5") int limit
    ) {
        return reportService.topCustomers(period, startDate, endDate, limit);
    }
}
