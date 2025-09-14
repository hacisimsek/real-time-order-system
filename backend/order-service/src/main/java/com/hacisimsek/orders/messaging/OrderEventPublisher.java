package com.hacisimsek.orders.messaging;

import com.hacisimsek.orders.domain.OrderItem;
import com.hacisimsek.orders.domain.OrderStatus;

import java.util.List;

public interface OrderEventPublisher {
    void orderCreated(Long id, String customerId, long amountCents, String currency, List<OrderItem> items);
    void orderStatusChanged(Long id, OrderStatus status, List<OrderItem> items);
}
