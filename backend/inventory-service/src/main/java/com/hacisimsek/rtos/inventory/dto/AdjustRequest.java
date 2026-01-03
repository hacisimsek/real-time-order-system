package com.hacisimsek.rtos.inventory.dto;

import jakarta.validation.constraints.NotNull;

public record AdjustRequest(@NotNull Integer delta, String reason) {}
