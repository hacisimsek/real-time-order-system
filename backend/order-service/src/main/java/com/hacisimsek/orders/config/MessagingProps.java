package com.hacisimsek.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProps(
        String exchange,
        Queues queues,
        RoutingKeys routingKeys
) {
    public record Queues(String orderCreated) {}
    public record RoutingKeys(String orderCreated, String orderStatusChanged) {}
}

