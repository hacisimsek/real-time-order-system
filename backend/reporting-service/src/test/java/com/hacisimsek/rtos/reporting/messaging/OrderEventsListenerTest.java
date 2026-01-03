package com.hacisimsek.rtos.reporting.messaging;

import com.hacisimsek.rtos.reporting.metrics.ReportingMetrics;
import com.hacisimsek.rtos.reporting.repository.MessageLogRepository;
import com.hacisimsek.rtos.reporting.service.OrderRollupService;
import com.rabbitmq.client.Channel;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderEventsListenerTest {

    @Test
    void duplicateMessageIsAckedAndSkipped() throws Exception {
        OrderRollupService rollupService = mock(OrderRollupService.class);
        MessageLogRepository repo = mock(MessageLogRepository.class);
        ReportingMetrics metrics = mock(ReportingMetrics.class);
        Channel channel = mock(Channel.class);

        OrderEventsListener listener = new OrderEventsListener(rollupService, repo, metrics);

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(11L);
        props.setHeader("x-event-id", "evt-1");
        Message msg = new Message(new byte[0], props);

        when(repo.existsById("evt-1")).thenReturn(true);

        listener.handleOrderCreated(Map.of("amountCents", 1000), msg, channel);

        verify(channel).basicAck(11L, false);
        verifyNoInteractions(rollupService);
        verify(metrics, never()).recordOrderFailed();
    }

    @Test
    void processingFailureIsNackedWithoutRequeueSoItCanDeadLetter() throws Exception {
        OrderRollupService rollupService = mock(OrderRollupService.class);
        MessageLogRepository repo = mock(MessageLogRepository.class);
        ReportingMetrics metrics = mock(ReportingMetrics.class);
        Channel channel = mock(Channel.class);

        OrderEventsListener listener = new OrderEventsListener(rollupService, repo, metrics);

        Instant eventTs = Instant.parse("2025-01-02T03:04:05Z");
        LocalDate expectedDate = eventTs.atOffset(ZoneOffset.UTC).toLocalDate();

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(22L);
        props.setHeader("x-event-id", "evt-2");
        props.setTimestamp(Date.from(eventTs));
        Message msg = new Message(new byte[0], props);

        when(repo.existsById("evt-2")).thenReturn(false);
        doThrow(new RuntimeException("db down")).when(rollupService).recordOrder(eq(expectedDate), eq(500L));

        listener.handleOrderCreated(Map.of("amountCents", 500), msg, channel);

        verify(metrics).recordOrderFailed();
        verify(channel).basicNack(22L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}

