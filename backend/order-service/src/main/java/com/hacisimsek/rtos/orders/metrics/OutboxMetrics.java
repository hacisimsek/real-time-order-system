package com.hacisimsek.rtos.orders.metrics;

import com.hacisimsek.rtos.orders.domain.OutboxStatus;
import com.hacisimsek.rtos.orders.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetrics {

    private final MeterRegistry registry;
    private final AtomicLong pendingGauge;
    private final OutboxEventRepository repository;

    public OutboxMetrics(MeterRegistry registry, OutboxEventRepository repository) {
        this.registry = registry;
        this.repository = repository;
        this.pendingGauge = new AtomicLong(0);
        Gauge.builder("order_outbox_pending_events", pendingGauge, AtomicLong::get)
                .description("Number of pending outbox events awaiting dispatch")
                .register(registry);
    }

    public void recordDispatched(String eventType) {
        counter("success", eventType).increment();
    }

    public void recordFailed(String eventType) {
        counter("failed", eventType).increment();
    }

    public void refreshPendingCount() {
        pendingGauge.set(repository.countByStatus(OutboxStatus.PENDING));
    }

    private Counter counter(String result, String eventType) {
        return Counter.builder("order_outbox_dispatch_total")
                .tag("result", result)
                .tag("eventType", eventType)
                .register(registry);
    }
}
