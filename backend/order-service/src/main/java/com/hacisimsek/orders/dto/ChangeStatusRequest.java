package com.hacisimsek.orders.dto;

import com.hacisimsek.orders.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull OrderStatus status) {}
