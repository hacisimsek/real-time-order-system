package com.hacisimsek.orders.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.TopicExchange;

@Configuration
@EnableConfigurationProperties(MessagingProps.class)
public class AmqpConfig {

    @Bean
    TopicExchange orderEventsExchange(MessagingProps props) {
        return new TopicExchange(props.exchange(), true, false);
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
