package com.hacisimsek.rtos.reporting.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.reporting.cache")
public class CacheProps {

    /**
     * Cache provider to use. Defaults to in-memory Caffeine; set to "redis" for distributed cache.
     */
    private String provider = "caffeine";

    /**
     * Cache entry time-to-live; defaults to one minute to balance freshness and performance.
     */
    private Duration ttl = Duration.ofSeconds(60);

    /**
     * Maximum number of cache entries per cache.
     */
    private long maximumSize = 500;

    public Duration getTtl() {
        return ttl;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        if (provider != null && !provider.isBlank()) {
            this.provider = provider.trim();
        }
    }

    public void setTtl(Duration ttl) {
        if (ttl != null) {
            this.ttl = ttl;
        }
    }

    public long getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(long maximumSize) {
        if (maximumSize > 0) {
            this.maximumSize = maximumSize;
        }
    }
}
