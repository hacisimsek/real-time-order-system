package com.hacisimsek.rtos.inventory.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReserveRequest(
        @NotBlank String sku,
        @NotNull @Min(1) Integer qty
) {}
