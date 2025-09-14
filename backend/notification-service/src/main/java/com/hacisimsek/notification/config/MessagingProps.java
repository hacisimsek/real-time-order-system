package com.hacisimsek.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProps(
        String exchange,
        RoutingKeys routingKeys,
        Queues queues
) {
    public record RoutingKeys(String orderCreated, String orderStatusChanged) {}
    public record Queues(String orderCreated, String orderStatusChanged) {}
}
