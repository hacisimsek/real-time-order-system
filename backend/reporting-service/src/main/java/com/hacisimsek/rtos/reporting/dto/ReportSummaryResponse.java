package com.hacisimsek.rtos.reporting.dto;

import com.hacisimsek.rtos.reporting.domain.ReportPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportSummaryResponse(
        ReportPeriod period,
        LocalDate snapshotDate,
        long totalOrders,
        long totalRevenueCents,
        BigDecimal totalRevenue
) { }
