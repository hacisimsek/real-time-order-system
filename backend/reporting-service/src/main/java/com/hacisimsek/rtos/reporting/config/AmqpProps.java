package com.hacisimsek.rtos.reporting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public record AmqpProps(
        String exchange,
        Queues queues,
        RoutingKeys routingKeys
) {
    public record Queues(String orderCreated) {}
    public record RoutingKeys(String orderCreated) {}
}
