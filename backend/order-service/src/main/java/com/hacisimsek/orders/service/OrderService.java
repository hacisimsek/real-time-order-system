package com.hacisimsek.orders.service;

import com.hacisimsek.orders.domain.Order;
import com.hacisimsek.orders.domain.OrderItem;
import com.hacisimsek.orders.domain.OrderStatus;
import com.hacisimsek.orders.dto.OrderCreateRequest;
import com.hacisimsek.orders.messaging.OrderEventItem;
import com.hacisimsek.orders.messaging.OrderEventPublisher;
import com.hacisimsek.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
        var items = saved.getItems().stream()
                .map(it -> new OrderEventItem(it.getSku(), it.getQty()))
                .toList();

        publishAfterCommit(() -> publisher.orderCreated(
                saved.getId(),
                saved.getCustomerId(),
                saved.getAmountCents(),
                saved.getCurrency(),
                items
        ));

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

        publishAfterCommit(() -> publisher.orderStatusChanged(
                order.getId(),
                status,
                items
        ));

        return order;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Order> getById(Long id) {
        return repo.findByIdWithItems(id);
    }

    private static void publishAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
