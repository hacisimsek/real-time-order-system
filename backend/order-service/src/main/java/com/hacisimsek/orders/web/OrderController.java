package com.hacisimsek.orders.web;

import com.hacisimsek.orders.domain.OrderStatus;
import com.hacisimsek.orders.dto.ChangeStatusRequest;
import com.hacisimsek.orders.dto.CreateOrderRequest;
import com.hacisimsek.orders.dto.OrderResponse;
import com.hacisimsek.orders.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> list(@RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(service.list(status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> changeStatus(@PathVariable Long id,
                                                      @Valid @RequestBody ChangeStatusRequest req) {
        return ResponseEntity.ok(service.changeStatus(id, req));
    }
}
