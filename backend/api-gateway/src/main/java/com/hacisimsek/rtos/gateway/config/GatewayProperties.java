package com.hacisimsek.rtos.gateway.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.gateway")
public class GatewayProperties {

    private List<String> publicPaths = new ArrayList<>(List.of("/actuator"));
    private List<String> allowedMethods = new ArrayList<>(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
    private long maxBodyBytes = 262_144;
    private RateLimits rateLimits = new RateLimits();

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths == null ? new ArrayList<>() : new ArrayList<>(publicPaths);
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods == null ? new ArrayList<>() : new ArrayList<>(allowedMethods);
    }

    public long getMaxBodyBytes() {
        return maxBodyBytes;
    }

    public void setMaxBodyBytes(long maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    public RateLimits getRateLimits() {
        return rateLimits;
    }

    public void setRateLimits(RateLimits rateLimits) {
        this.rateLimits = rateLimits == null ? new RateLimits() : rateLimits;
    }

    public static class RateLimits {
        private RateLimitPolicy orders = RateLimitPolicy.defaults(120, 10, 1200);
        private RateLimitPolicy inventory = RateLimitPolicy.defaults(240, 10, 2400);
        private RateLimitPolicy reports = RateLimitPolicy.defaults(120, 10, 1200);
        private RateLimitPolicy fallback = RateLimitPolicy.defaults(60, 10, 600);

        public RateLimitPolicy getOrders() {
            return orders;
        }

        public void setOrders(RateLimitPolicy orders) {
            this.orders = orders == null ? RateLimitPolicy.defaults(120, 10, 1200) : orders;
        }

        public RateLimitPolicy getInventory() {
            return inventory;
        }

        public void setInventory(RateLimitPolicy inventory) {
            this.inventory = inventory == null ? RateLimitPolicy.defaults(240, 10, 2400) : inventory;
        }

        public RateLimitPolicy getReports() {
            return reports;
        }

        public void setReports(RateLimitPolicy reports) {
            this.reports = reports == null ? RateLimitPolicy.defaults(120, 10, 1200) : reports;
        }

        public RateLimitPolicy getFallback() {
            return fallback;
        }

        public void setFallback(RateLimitPolicy fallback) {
            this.fallback = fallback == null ? RateLimitPolicy.defaults(60, 10, 600) : fallback;
        }
    }

    public static class RateLimitPolicy {
        private int perMinute = 60;
        private int burstSeconds = 10;
        private int globalPerMinute = 0;

        public RateLimitPolicy() {
        }

        public RateLimitPolicy(int perMinute, int burstSeconds, int globalPerMinute) {
            this.perMinute = perMinute;
            this.burstSeconds = burstSeconds;
            this.globalPerMinute = globalPerMinute;
        }

        public static RateLimitPolicy defaults(int perMinute, int burstSeconds, int globalPerMinute) {
            return new RateLimitPolicy(perMinute, burstSeconds, globalPerMinute);
        }

        public int getPerMinute() {
            return perMinute;
        }

        public void setPerMinute(int perMinute) {
            this.perMinute = perMinute;
        }

        public int getBurstSeconds() {
            return burstSeconds;
        }

        public void setBurstSeconds(int burstSeconds) {
            this.burstSeconds = burstSeconds;
        }

        public int getGlobalPerMinute() {
            return globalPerMinute;
        }

        public void setGlobalPerMinute(int globalPerMinute) {
            this.globalPerMinute = globalPerMinute;
        }
    }
}
