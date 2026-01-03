package com.hacisimsek.rtos.reporting.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportingMetricsTest {

    private SimpleMeterRegistry registry;
    private ReportingMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ReportingMetrics(registry);
    }

    @Test
    void recordsOrderProcessingData() {
        Instant eventTs = Instant.now().minusSeconds(5);
        Instant processedTs = Instant.now();

        metrics.recordOrderProcessed(1234, eventTs, processedTs);

        double count = registry.get("reporting_orders_processed_total").counter().count();
        assertThat(count).isEqualTo(1d);

        double amount = registry.get("reporting_order_amount_cents").summary().totalAmount();
        assertThat(amount).isEqualTo(1234d);

        double epoch = registry.get("reporting_last_order_timestamp_seconds").gauge().value();
        assertThat(epoch).isEqualTo(processedTs.getEpochSecond());
    }
}
