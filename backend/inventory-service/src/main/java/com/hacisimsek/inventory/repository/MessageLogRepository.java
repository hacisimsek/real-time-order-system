package com.hacisimsek.inventory.repository;

import com.hacisimsek.inventory.domain.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {
    boolean existsByMessageId(String messageId);
}
