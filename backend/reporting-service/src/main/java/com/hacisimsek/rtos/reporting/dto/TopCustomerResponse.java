package com.hacisimsek.rtos.reporting.dto;

import java.math.BigDecimal;

public record TopCustomerResponse(
        String customerId,
        long totalOrders,
        long totalRevenueCents,
        BigDecimal totalRevenue
) {}
