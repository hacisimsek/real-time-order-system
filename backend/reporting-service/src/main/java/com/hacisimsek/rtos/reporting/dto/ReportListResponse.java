package com.hacisimsek.rtos.reporting.dto;

import java.util.List;

public record ReportListResponse(
        List<ReportSummaryResponse> items,
        PageMetadata page,
        ReportTotalsResponse totals
) {
    public record PageMetadata(int page, int size, long totalElements, int totalPages) {}
}
