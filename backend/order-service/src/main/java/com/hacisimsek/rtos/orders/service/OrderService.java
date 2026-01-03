package com.hacisimsek.rtos.orders.service;

import com.hacisimsek.rtos.orders.domain.Order;
import com.hacisimsek.rtos.orders.domain.OrderItem;
import com.hacisimsek.rtos.orders.domain.OrderStatus;
import com.hacisimsek.rtos.orders.dto.OrderCreateRequest;
import com.hacisimsek.rtos.orders.messaging.OrderEventItem;
import com.hacisimsek.rtos.orders.messaging.OrderEventPublisher;
import com.hacisimsek.rtos.orders.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository repo;
    private final OrderEventPublisher publisher;

    public OrderService(OrderRepository repo,
                        @Qualifier("outboxOrderEventPublisher") OrderEventPublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    @Transactional
    public Order create(OrderCreateRequest req) {
        var o = new Order();
        o.setCustomerId(req.customerId());
        o.setAmountCents(req.amountCents());
        o.setCurrency(req.currency());
        o.setStatus(OrderStatus.CREATED);

        for (var it : req.items()) {
            var oi = new OrderItem();
            oi.setSku(it.sku());
            oi.setQty(it.qty());
            o.addItem(oi);
        }

        var saved = repo.save(o);
        var items = saved.getItems().stream()
                .map(it -> new OrderEventItem(it.getSku(), it.getQty()))
                .toList();

        publisher.orderCreated(
                saved.getId(),
                saved.getCustomerId(),
                saved.getAmountCents(),
                saved.getCurrency(),
                items
        );

        return saved;
    }

    @Transactional
    public Order changeStatus(Long id, OrderStatus newStatus) {
        var order = repo.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        order.setStatus(newStatus);

        var status = order.getStatus();
        var items = order.getItems().stream()
                .map(it -> new OrderEventItem(it.getSku(), it.getQty()))
                .toList();

        publisher.orderStatusChanged(
                order.getId(),
                status,
                items
        );

        return order;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Order> getById(Long id) {
        return repo.findByIdWithItems(id);
    }
}
