package com.hacisimsek.inventory.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AmqpProps.class)
public class AmqpConfig {

    @Bean
    TopicExchange orderExchange(AmqpProps p) {
        return new TopicExchange(p.exchange(), true, false);
    }

    @Bean
    Queue orderCreatedQ(AmqpProps p) {
        return QueueBuilder.durable(p.queues().orderCreated()).build();
    }

    @Bean
    Queue orderStatusChangedQ(AmqpProps p) {
        return QueueBuilder.durable(p.queues().orderStatusChanged()).build();
    }

    @Bean
    Binding bindCreated(Queue orderCreatedQ, TopicExchange orderExchange, AmqpProps p) {
        return BindingBuilder.bind(orderCreatedQ).to(orderExchange).with(p.routingKeys().orderCreated());
    }

    @Bean
    Binding bindStatusChanged(Queue orderStatusChangedQ, TopicExchange orderExchange, AmqpProps p) {
        return BindingBuilder.bind(orderStatusChangedQ).to(orderExchange).with(p.routingKeys().orderStatusChanged());
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    SimpleRabbitListenerContainerFactory manualAckContainerFactory(
            ConnectionFactory cf,
            Jackson2JsonMessageConverter converter
    ) {
        var f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(converter);
        f.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        f.setDefaultRequeueRejected(false); // hata olursa DLQ/ignore, kuyruğa geri koyma
        return f;
    }
}
