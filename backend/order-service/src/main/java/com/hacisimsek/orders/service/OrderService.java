package com.hacisimsek.orders.service;

import com.hacisimsek.orders.domain.Order;
import com.hacisimsek.orders.domain.OrderItem;
import com.hacisimsek.orders.domain.OrderStatus;
import com.hacisimsek.orders.dto.OrderCreateRequest;
import com.hacisimsek.orders.messaging.OrderEventPublisher;
import com.hacisimsek.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository repo;
    private final OrderEventPublisher publisher;

    public OrderService(OrderRepository repo, OrderEventPublisher publisher) {
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

        publisher.orderCreated(
                saved.getId(),
                saved.getCustomerId(),
                saved.getAmountCents(),
                saved.getCurrency(),
                saved.getItems()
        );

        return saved;
    }

    @Transactional
    public Order changeStatus(Long id, OrderStatus newStatus) {
        var order = repo.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        order.setStatus(newStatus);

        publisher.orderStatusChanged(
                order.getId(),
                order.getStatus(),
                order.getItems()
        );

        return order;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Order> getById(Long id) {
        return repo.findByIdWithItems(id);
    }
}
