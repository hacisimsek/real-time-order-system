package com.hacisimsek.orders.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private final MeterRegistry reg;
    public MetricsService(MeterRegistry reg) { this.reg = reg; }

    public void incPublished(String type) {
        Counter.builder("order_events_published_total").tag("type", type).register(reg).increment();
    }
    public void incPublishFail(String type) {
        Counter.builder("order_events_publish_failed_total").tag("type", type).register(reg).increment();
    }
}
