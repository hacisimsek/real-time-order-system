package com.hacisimsek.orders.messaging;

import com.hacisimsek.orders.domain.OrderStatus;

public interface OrderEventPublisher {
    void orderCreated(Long id, String customerId, long amountCents, String currency);
    void orderStatusChanged(Long id, OrderStatus status);
}
