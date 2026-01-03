package com.hacisimsek.rtos.orders.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotBlank @Size(max = 64) String customerId,
        @Min(1) long amountCents,
        @NotBlank @Size(min = 3, max = 3) String currency
) {}
