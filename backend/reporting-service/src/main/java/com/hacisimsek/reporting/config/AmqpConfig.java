package com.hacisimsek.reporting.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@Configuration
@EnableConfigurationProperties(AmqpProps.class)
public class AmqpConfig {

    public static final String DLX_EXCHANGE = "reporting.dlx";

    @Bean
    TopicExchange orderEventsExchange(AmqpProps props) {
        return new TopicExchange(props.exchange(), true, false);
    }

    @Bean
    TopicExchange dlx() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    Queue orderCreatedQueue(AmqpProps props) {
        return QueueBuilder.durable(props.queues().orderCreated())
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", props.routingKeys().orderCreated() + ".dlq")
                .build();
    }

    @Bean
    Queue orderCreatedDlq(AmqpProps props) {
        return QueueBuilder.durable(props.queues().orderCreated() + ".dlq").build();
    }

    @Bean
    Binding bindOrderCreated(Queue orderCreatedQueue, TopicExchange orderEventsExchange, AmqpProps props) {
        return BindingBuilder.bind(orderCreatedQueue).to(orderEventsExchange).with(props.routingKeys().orderCreated());
    }

    @Bean
    Binding bindOrderCreatedDlq(Queue orderCreatedDlq, TopicExchange dlx, AmqpProps props) {
        return BindingBuilder.bind(orderCreatedDlq).to(dlx).with(props.routingKeys().orderCreated() + ".dlq");
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    SimpleRabbitListenerContainerFactory manualAckContainerFactory(ConnectionFactory connectionFactory,
                                                                   Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    RabbitAdmin reportingRabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
