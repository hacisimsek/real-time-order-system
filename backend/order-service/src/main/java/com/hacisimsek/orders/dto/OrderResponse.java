package com.hacisimsek.orders.dto;

import com.hacisimsek.orders.domain.OrderStatus;
import java.time.OffsetDateTime;

public record OrderResponse(
        Long id, String customerId, long amountCents, String currency,
        OrderStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {}

