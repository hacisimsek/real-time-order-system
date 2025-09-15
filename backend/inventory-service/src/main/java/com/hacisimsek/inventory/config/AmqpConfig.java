package com.hacisimsek.inventory.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AmqpProps.class)
public class AmqpConfig {

    public static final String RETRY_EXCHANGE = "inventory.retry";
    public static final String DLX_EXCHANGE   = "inventory.dlx";

    @Value("${app.messaging.retryTtlMs:10000}")
    private long retryTtlMs;

    @Bean TopicExchange retryExchange() { return new TopicExchange(RETRY_EXCHANGE, true, false); }
    @Bean TopicExchange dlx()          { return new TopicExchange(DLX_EXCHANGE,   true, false); }

    @Bean Queue orderCreatedRetryQ(AmqpProps p){
        return QueueBuilder.durable(p.queues().orderCreated() + ".retry")
                .withArgument("x-message-ttl", retryTtlMs)
                .withArgument("x-dead-letter-exchange", p.exchange())
                .withArgument("x-dead-letter-routing-key", p.routingKeys().orderCreated())
                .build();
    }
    @Bean Queue orderStatusChangedRetryQ(AmqpProps p){
        return QueueBuilder.durable(p.queues().orderStatusChanged() + ".retry")
                .withArgument("x-message-ttl", retryTtlMs)
                .withArgument("x-dead-letter-exchange", p.exchange())
                .withArgument("x-dead-letter-routing-key", p.routingKeys().orderStatusChanged())
                .build();
    }

    @Bean Binding bindOrderCreatedRetry(Queue orderCreatedRetryQ, TopicExchange retryExchange, AmqpProps p){
        return BindingBuilder.bind(orderCreatedRetryQ).to(retryExchange).with(p.routingKeys().orderCreated() + ".retry");
    }
    @Bean Binding bindOrderStatusChangedRetry(Queue orderStatusChangedRetryQ, TopicExchange retryExchange, AmqpProps p){
        return BindingBuilder.bind(orderStatusChangedRetryQ).to(retryExchange).with(p.routingKeys().orderStatusChanged() + ".retry");
    }

    @Bean Queue orderCreatedDlq(AmqpProps p){
        return QueueBuilder.durable(p.queues().orderCreated() + ".dlq").build();
    }
    @Bean Queue orderStatusChangedDlq(AmqpProps p){
        return QueueBuilder.durable(p.queues().orderStatusChanged() + ".dlq").build();
    }
    @Bean Binding bindCreatedDlq(Queue orderCreatedDlq, TopicExchange dlx, AmqpProps p){
        return BindingBuilder.bind(orderCreatedDlq).to(dlx).with(p.routingKeys().orderCreated() + ".dlq");
    }
    @Bean Binding bindStatusChangedDlq(Queue orderStatusChangedDlq, TopicExchange dlx, AmqpProps p){
        return BindingBuilder.bind(orderStatusChangedDlq).to(dlx).with(p.routingKeys().orderStatusChanged() + ".dlq");
    }

    @Bean Queue orderCreatedQ(AmqpProps p){
        return QueueBuilder.durable(p.queues().orderCreated())
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", p.routingKeys().orderCreated() + ".dlq")
                .build();
    }
    @Bean Queue orderStatusChangedQ(AmqpProps p){
        return QueueBuilder.durable(p.queues().orderStatusChanged())
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", p.routingKeys().orderStatusChanged() + ".dlq")
                .build();
    }
}
