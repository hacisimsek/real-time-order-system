package com.hacisimsek.orders.web;

import com.hacisimsek.orders.domain.Order;
import com.hacisimsek.orders.domain.OrderStatus;
import com.hacisimsek.orders.dto.OrderCreateRequest;
import com.hacisimsek.orders.service.OrderService;
import com.hacisimsek.security.Roles;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Roles.ORDER_WRITE + "')")
    public ResponseEntity<Order> create(@Valid @RequestBody OrderCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + Roles.ORDER_READ + "','" + Roles.ORDER_WRITE + "')")
    public ResponseEntity<Order> get(@PathVariable Long id) {
        return ResponseEntity.of(service.getById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + Roles.ORDER_WRITE + "')")
    public ResponseEntity<Order> changeStatus(@PathVariable Long id, @RequestBody StatusRequest body) {
        return ResponseEntity.ok(service.changeStatus(id, body.status()));
    }

    public record StatusRequest(OrderStatus status) {}
}
