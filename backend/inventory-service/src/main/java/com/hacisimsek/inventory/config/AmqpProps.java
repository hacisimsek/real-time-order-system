package com.hacisimsek.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public record AmqpProps(
        String exchange,
        Queues queues,
        RoutingKeys routingKeys
) {
    public record Queues(String orderCreated, String orderStatusChanged) {}
    public record RoutingKeys(String orderCreated, String orderStatusChanged) {}
}
