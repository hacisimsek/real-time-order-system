package com.hacisimsek.orders.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MessagingProps.class)
public class AmqpConfig {

    @Bean
    TopicExchange orderExchange(MessagingProps props) {
        return new TopicExchange(props.exchange(), true, false);
    }

    @Bean
    Queue orderCreatedQueue(MessagingProps props) {
        return QueueBuilder.durable(props.queues().orderCreated()).build();
    }

    @Bean
    Binding bindOrderCreated(Queue orderCreatedQueue, TopicExchange orderExchange, MessagingProps props) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(orderExchange)
                .with(props.routingKeys().orderCreated());
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(conv);
        return t;
    }
}
