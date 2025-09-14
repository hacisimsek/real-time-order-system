package com.hacisimsek.orders.service;

import com.hacisimsek.orders.domain.Order;
import com.hacisimsek.orders.domain.OrderStatus;
import com.hacisimsek.orders.dto.ChangeStatusRequest;
import com.hacisimsek.orders.dto.CreateOrderRequest;
import com.hacisimsek.orders.dto.OrderResponse;
import com.hacisimsek.orders.messaging.OrderEventPublisher;
import com.hacisimsek.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository repo;
    private final OrderEventPublisher publisher;

    public OrderService(OrderRepository repo, OrderEventPublisher publisher) {
        this.repo = repo; this.publisher = publisher;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest req) {
        Order o = new Order();
        o.setCustomerId(req.customerId());
        o.setAmountCents(req.amountCents());
        o.setCurrency(req.currency());
        o.setStatus(OrderStatus.CREATED);
        Order saved = repo.save(o);
        publisher.orderCreated(saved.getId(), saved.getCustomerId(), saved.getAmountCents(), saved.getCurrency());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        Order o = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return toDto(o);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(OrderStatus status) {
        List<Order> list = (status == null) ? repo.findAll() : repo.findByStatus(status);
        return list.stream().map(this::toDto).toList();
    }

    @Transactional
    public OrderResponse changeStatus(Long id, ChangeStatusRequest req) {
        Order o = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        o.setStatus(req.status());
        Order saved = repo.save(o);
        publisher.orderStatusChanged(saved.getId(), saved.getStatus());
        return toDto(saved);
    }

    private OrderResponse toDto(Order o) {
        return new OrderResponse(o.getId(), o.getCustomerId(), o.getAmountCents(),
                o.getCurrency(), o.getStatus(), o.getCreatedAt(), o.getUpdatedAt());
    }
}

