package com.hacisimsek.rtos.reporting.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class ReportingMetrics {

    private final Counter ordersProcessed;
    private final Counter ordersFailed;
    private final DistributionSummary orderAmountSummary;
    private final Timer processingLatency;
    private final AtomicLong lastOrderTimestampSeconds;

    public ReportingMetrics(MeterRegistry registry) {
        this.ordersProcessed = Counter.builder("reporting_orders_processed_total")
                .description("Total number of order events processed by the reporting service")
                .tag("source", "order.events")
                .register(registry);

        this.ordersFailed = Counter.builder("reporting_orders_failed_total")
                .description("Total number of order events that failed processing in reporting")
                .tag("source", "order.events")
                .tag("reason", "exception")
                .register(registry);

        this.orderAmountSummary = DistributionSummary.builder("reporting_order_amount_cents")
                .description("Distribution of order amounts processed by reporting")
                .baseUnit("cents")
                .scale(1.0)
                .register(registry);

        this.processingLatency = Timer.builder("reporting_order_processing_latency")
                .description("Latency between order event timestamp and processing completion")
                .publishPercentiles(0.5, 0.9, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        this.lastOrderTimestampSeconds = registry.gauge(
                "reporting_last_order_timestamp_seconds",
                new AtomicLong(0));
    }

    public void recordOrderProcessed(long amountCents, Instant eventTimestamp, Instant processedTimestamp) {
        ordersProcessed.increment();
        orderAmountSummary.record(amountCents);

        Instant eventTs = eventTimestamp != null ? eventTimestamp : processedTimestamp;
        Duration latency = Duration.between(eventTs, processedTimestamp);
        if (!latency.isNegative()) {
            processingLatency.record(latency);
        }

        lastOrderTimestampSeconds.set(processedTimestamp.getEpochSecond());
    }

    public void recordOrderFailed() {
        ordersFailed.increment();
    }
}
