package com.hacisimsek.reporting.dto;

import com.hacisimsek.reporting.domain.ReportPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportSummaryResponse(
        ReportPeriod period,
        LocalDate snapshotDate,
        long totalOrders,
        long totalRevenueCents,
        BigDecimal totalRevenue
) { }
