package com.hacisimsek.rtos.notification.metrics;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private final MeterRegistry reg;
    public MetricsService(MeterRegistry reg) { this.reg = reg; }

    public void incProcessed(String type) {
        Counter.builder("notification_messages_processed_total")
                .tag("type", type).register(reg).increment();
    }
}
