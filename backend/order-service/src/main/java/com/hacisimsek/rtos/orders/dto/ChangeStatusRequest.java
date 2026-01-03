package com.hacisimsek.rtos.orders.dto;

import com.hacisimsek.rtos.orders.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull OrderStatus status) {}
