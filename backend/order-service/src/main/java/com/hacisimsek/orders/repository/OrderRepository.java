package com.hacisimsek.orders.repository;

import com.hacisimsek.orders.domain.Order;
import com.hacisimsek.orders.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);
}
