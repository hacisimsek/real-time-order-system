package com.hacisimsek.rtos.inventory.ops;


import com.hacisimsek.rtos.inventory.config.AmqpConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqReplayer {
    private final RabbitTemplate rabbit;
    private final RabbitAdmin admin;


    public int replay(String dlqName, int maxCount) {
        int moved = 0;
        for (; moved < maxCount; moved++) {
            Message m = rabbit.receive(dlqName, 1000);
            if (m == null) break;

            String dlqRk = (String) m.getMessageProperties().getReceivedRoutingKey();
            String base = dlqRk != null && dlqRk.endsWith(".dlq")
                    ? dlqRk.substring(0, dlqRk.length() - 4)
                    : "order.created.v1";

            String retryRk = base + ".retry";

            rabbit.send(AmqpConfig.RETRY_EXCHANGE, retryRk, m);
        }
        try {
            var info = admin.getQueueInfo(dlqName);
            log.info("DLQ replay done. moved={}, remaining={}", moved, info != null ? info.getMessageCount() : "?");
        } catch (Exception ignore) {}
        return moved;
    }
}
