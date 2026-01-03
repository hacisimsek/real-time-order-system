package com.hacisimsek.rtos.orders.messaging;

import com.hacisimsek.rtos.orders.domain.OrderStatus;

import java.util.List;

public interface OrderEventPublisher {
    void orderCreated(Long id, String customerId, long amountCents, String currency, List<OrderEventItem> items);
    void orderStatusChanged(Long id, OrderStatus status, List<OrderEventItem> items);
}
