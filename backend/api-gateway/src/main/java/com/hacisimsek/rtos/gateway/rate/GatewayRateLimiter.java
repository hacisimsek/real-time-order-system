package com.hacisimsek.rtos.gateway.rate;

import com.hacisimsek.rtos.gateway.config.GatewayProperties;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class GatewayRateLimiter {

    private final GatewayProperties properties;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public GatewayRateLimiter(GatewayProperties properties) {
        this.properties = properties;
    }

    public boolean tryConsume(String routeKey, String clientKey) {
        GatewayProperties.RateLimitPolicy policy = policyFor(routeKey);
        if (policy == null || policy.getPerMinute() <= 0) {
            return true;
        }
        boolean clientAllowed = tryConsume(bucketKey(routeKey, clientKey),
                policy.getPerMinute(),
                policy.getBurstSeconds());

        boolean globalAllowed = true;
        if (policy.getGlobalPerMinute() > 0) {
            globalAllowed = tryConsume(bucketKey(routeKey, "global"),
                    policy.getGlobalPerMinute(),
                    policy.getBurstSeconds());
        }
        return clientAllowed && globalAllowed;
    }

    private boolean tryConsume(String key, int perMinute, int burstSeconds) {
        if (perMinute <= 0) {
            return true;
        }
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> TokenBucket.create(perMinute, burstSeconds));
        return bucket.tryConsume();
    }

    private GatewayProperties.RateLimitPolicy policyFor(String routeKey) {
        if (routeKey == null) {
            return properties.getRateLimits().getFallback();
        }
        return switch (routeKey) {
            case "orders" -> properties.getRateLimits().getOrders();
            case "inventory" -> properties.getRateLimits().getInventory();
            case "reports" -> properties.getRateLimits().getReports();
            default -> properties.getRateLimits().getFallback();
        };
    }

    private String bucketKey(String routeKey, String clientKey) {
        return routeKey + ":" + clientKey;
    }
}
