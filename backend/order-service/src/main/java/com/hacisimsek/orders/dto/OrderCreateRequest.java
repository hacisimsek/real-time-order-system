package com.hacisimsek.orders.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderCreateRequest(
        @NotBlank String customerId,
        @Min(0) long amountCents,
        @NotBlank String currency,
        @NotNull @Size(min = 1) List<Item> items
) {
    public record Item(@NotBlank String sku, @Min(1) int qty) {}
}
