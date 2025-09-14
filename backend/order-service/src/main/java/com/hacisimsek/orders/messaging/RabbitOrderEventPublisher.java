package com.hacisimsek.orders.messaging;

import com.hacisimsek.orders.config.MessagingProps;
import com.hacisimsek.orders.domain.OrderStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Primary
public class RabbitOrderEventPublisher implements OrderEventPublisher {

    private final RabbitTemplate rabbit;
    private final MessagingProps props;

    public RabbitOrderEventPublisher(RabbitTemplate rabbit, MessagingProps props) {
        this.rabbit = rabbit;
        this.props = props;
    }

    @Override
    public void orderCreated(Long id, String customerId, long amountCents, String currency) {
        Map<String, Object> payload = Map.of(
                "type", "order.created.v1",
                "id", id,
                "customerId", customerId,
                "amountCents", amountCents,
                "currency", currency
        );
        rabbit.convertAndSend(props.exchange(), props.routingKeys().orderCreated(), payload);
    }

    @Override
    public void orderStatusChanged(Long id, OrderStatus status) {
        Map<String, Object> payload = Map.of(
                "type", "order.status-changed.v1",
                "id", id,
                "status", status.name()
        );
        rabbit.convertAndSend(props.exchange(), props.routingKeys().orderStatusChanged(), payload);
    }
}
