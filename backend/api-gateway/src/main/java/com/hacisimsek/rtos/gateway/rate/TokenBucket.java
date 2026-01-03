package com.hacisimsek.rtos.gateway.rate;

final class TokenBucket {

    private final double capacity;
    private final double refillPerNano;
    private double tokens;
    private long lastRefillNanos;

    private TokenBucket(double capacity, double refillPerSecond) {
        this.capacity = Math.max(1d, capacity);
        this.refillPerNano = refillPerSecond / 1_000_000_000d;
        this.tokens = this.capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    static TokenBucket create(int perMinute, int burstSeconds) {
        double perSecond = perMinute / 60d;
        double cap = perSecond * Math.max(1, burstSeconds);
        return new TokenBucket(cap, perSecond);
    }

    synchronized boolean tryConsume() {
        refill();
        if (tokens < 1d) {
            return false;
        }
        tokens -= 1d;
        return true;
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillNanos;
        if (elapsed <= 0) {
            return;
        }
        double newTokens = elapsed * refillPerNano;
        if (newTokens > 0d) {
            tokens = Math.min(capacity, tokens + newTokens);
            lastRefillNanos = now;
        }
    }
}
