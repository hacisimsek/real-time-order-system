package com.hacisimsek.reporting.ops;

import com.hacisimsek.reporting.config.AmqpProps;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DlqReplayer {

    private static final Logger log = LoggerFactory.getLogger(DlqReplayer.class);

    private final RabbitTemplate rabbit;
    private final RabbitAdmin admin;
    private final AmqpProps props;

    public DlqReplayer(RabbitTemplate rabbit, RabbitAdmin admin, AmqpProps props) {
        this.rabbit = rabbit;
        this.admin = admin;
        this.props = props;
    }

    public int replay(String dlqName, int maxCount) {
        int moved = 0;
        for (; moved < maxCount; moved++) {
            Message message = rabbit.receive(dlqName, 1000);
            if (message == null) {
                break;
            }

            String dlqRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
            String baseRoutingKey = dlqRoutingKey != null && dlqRoutingKey.endsWith(".dlq")
                    ? dlqRoutingKey.substring(0, dlqRoutingKey.length() - 4)
                    : props.routingKeys().orderCreated();

            rabbit.send(props.exchange(), baseRoutingKey, message);
        }

        try {
            var info = admin.getQueueInfo(dlqName);
            log.info("Reporting DLQ replay done. moved={}, remaining={}", moved, info != null ? info.getMessageCount() : "?");
        } catch (Exception ignore) { }

        return moved;
    }
}
