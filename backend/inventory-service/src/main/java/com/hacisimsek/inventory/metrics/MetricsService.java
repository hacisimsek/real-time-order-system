package com.hacisimsek.inventory.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;


@Service
public class MetricsService {

    private final MeterRegistry registry;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void incProcessed(String type) {
        registry.counter("inventory_messages_processed_total", "type", type).increment();
    }

    public void incFailed(String type, String reason) {
        registry.counter("inventory_messages_failed_total", "type", type, "reason", reason).increment();
    }

    public void incRetried(String type) {
        registry.counter("inventory_messages_retried_total", "type", type).increment();
    }

    public void incDlq(String type) {
        registry.counter("inventory_messages_dlq_total", "type", type).increment();
    }
}
