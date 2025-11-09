package com.hacisimsek.orders.repository;

import com.hacisimsek.orders.domain.OutboxEvent;
import com.hacisimsek.orders.domain.OutboxStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    long countByStatus(OutboxStatus status);
}
