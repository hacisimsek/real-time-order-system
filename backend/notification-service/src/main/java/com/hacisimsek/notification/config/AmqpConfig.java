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
    public static final String DLX = "order.dlx";

    @Bean TopicExchange orderExchange(MessagingProps p) { return new TopicExchange(p.exchange(), true, false);}
    @Bean TopicExchange dlx(){ return new TopicExchange(DLX, true,false); }

    @Bean
    Queue orderCreatedQueue(MessagingProps p) {
        return QueueBuilder.durable(p.queues().orderCreated())
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", p.routingKeys().orderCreated()+".dlq")
                .build();
    }

    @Bean
    Queue orderStatusChangedQueue(MessagingProps p) {
        return QueueBuilder.durable(p.queues().orderStatusChanged())
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", p.routingKeys().orderStatusChanged()+".dlq")
                .build();
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

    @Bean Queue orderCreatedDlq(MessagingProps p){ return QueueBuilder.durable(p.dlq().orderCreated()).build(); }
    @Bean Queue orderStatusChangedDlq(MessagingProps p){ return QueueBuilder.durable(p.dlq().orderStatusChanged()).build(); }

    @Bean Binding bindCreatedDlq(Queue orderCreatedDlq, TopicExchange dlx, MessagingProps p){
        return BindingBuilder.bind(orderCreatedDlq).to(dlx).with(p.routingKeys().orderCreated()+".dlq");
    }
    @Bean Binding bindStatusChangedDlq(Queue orderStatusChangedDlq, TopicExchange dlx, MessagingProps p){
        return BindingBuilder.bind(orderStatusChangedDlq).to(dlx).with(p.routingKeys().orderStatusChanged()+".dlq");
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    SimpleRabbitListenerContainerFactory manualAckContainerFactory(
            ConnectionFactory cf,
            Jackson2JsonMessageConverter conv
    ) {
        var f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(conv);
        f.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        f.setDefaultRequeueRejected(false);
        return f;
    }
}
