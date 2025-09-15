package com.hacisimsek.inventory.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private final MeterRegistry registry;
    public MetricsService(MeterRegistry registry) { this.registry = registry; }

    public void incProcessed(String type) {
        Counter.builder("inventory_messages_processed_total")
                .tag("type", type).register(registry).increment();
    }
    public void incRetried(String type) {
        Counter.builder("inventory_messages_retried_total")
                .tag("type", type).register(registry).increment();
    }
    public void incDlq(String type) {
        Counter.builder("inventory_messages_dlq_total")
                .tag("type", type).register(registry).increment();
    }
}
