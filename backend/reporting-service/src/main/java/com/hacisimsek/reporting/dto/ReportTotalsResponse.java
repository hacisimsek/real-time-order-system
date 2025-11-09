package com.hacisimsek.reporting.dto;

import com.hacisimsek.reporting.domain.ReportPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportTotalsResponse(
        ReportPeriod period,
        LocalDate startDate,
        LocalDate endDate,
        long totalOrders,
        long totalRevenueCents,
        BigDecimal totalRevenue
) {}
