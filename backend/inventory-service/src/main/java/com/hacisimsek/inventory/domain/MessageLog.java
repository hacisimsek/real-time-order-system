package com.hacisimsek.inventory.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_message_log", indexes = {
        @Index(name = "ux_inventory_message_log_mid", columnList = "messageId", unique = true)
})
public class MessageLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String messageId;
    @Column(nullable = false)
    private OffsetDateTime processedAt = OffsetDateTime.now();

    public MessageLog() {}
    public MessageLog(String messageId) { this.messageId = messageId; }

    public Long getId() { return id; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}
