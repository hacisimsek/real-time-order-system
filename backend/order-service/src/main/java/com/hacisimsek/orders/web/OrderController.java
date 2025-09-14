package com.hacisimsek.orders.web;

import com.hacisimsek.orders.domain.Order;
import com.hacisimsek.orders.domain.OrderStatus;
import com.hacisimsek.orders.dto.OrderCreateRequest;
import com.hacisimsek.orders.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody OrderCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> get(@PathVariable Long id) {
        return ResponseEntity.of(service.getById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> changeStatus(@PathVariable Long id, @RequestBody StatusRequest body) {
        return ResponseEntity.ok(service.changeStatus(id, body.status()));
    }

    public record StatusRequest(OrderStatus status) {}
}
