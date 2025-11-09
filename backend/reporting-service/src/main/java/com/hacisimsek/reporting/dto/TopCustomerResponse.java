package com.hacisimsek.reporting.dto;

import java.math.BigDecimal;

public record TopCustomerResponse(
        String customerId,
        long totalOrders,
        long totalRevenueCents,
        BigDecimal totalRevenue
) {}
