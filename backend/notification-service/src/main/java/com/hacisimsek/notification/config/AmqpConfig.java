package com.hacisimsek.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MessagingProps.class)
public class AmqpConfig {

    @Bean
    TopicExchange orderExchange(MessagingProps p) {
        return new TopicExchange(p.exchange(), true, false);
    }

    // Tüketilecek iki kuyruk (created + status-changed)
    @Bean
    Queue orderCreatedQueue(MessagingProps p) {
        return QueueBuilder.durable(p.queues().orderCreated()).build();
    }

    @Bean
    Queue orderStatusChangedQueue(MessagingProps p) {
        return QueueBuilder.durable(p.queues().orderStatusChanged()).build();
    }

    @Bean
    Binding bindCreated(Queue orderCreatedQueue, TopicExchange orderExchange, MessagingProps p) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(orderExchange)
                .with(p.routingKeys().orderCreated());
    }

    @Bean
    Binding bindStatusChanged(Queue orderStatusChangedQueue, TopicExchange orderExchange, MessagingProps p) {
        return BindingBuilder.bind(orderStatusChangedQueue)
                .to(orderExchange)
                .with(p.routingKeys().orderStatusChanged());
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Manuel ack için listener factory
    @Bean
    SimpleRabbitListenerContainerFactory manualAckContainerFactory(
            ConnectionFactory cf,
            Jackson2JsonMessageConverter conv
    ) {
        var f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(conv);
        f.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        f.setDefaultRequeueRejected(false); // hata olursa requeue etme (ileride DLQ ekleriz)
        f.setConcurrentConsumers(1);
        f.setMaxConcurrentConsumers(5);
        return f;
    }
}
