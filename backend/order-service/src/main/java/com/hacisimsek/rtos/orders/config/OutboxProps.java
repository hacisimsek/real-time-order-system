package com.hacisimsek.rtos.orders.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProps {

    /**
     * Number of pending events to process in a single scheduler tick.
     */
    private int batchSize = 50;

    /**
     * Maximum number of attempts before marking the event as dead.
     */
    private int maxAttempts = 5;

    /**
     * Poll interval for the outbox scheduler. (Used via application.yml for @Scheduled.)
     */
    private Duration pollInterval = Duration.ofSeconds(1);

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }
}
