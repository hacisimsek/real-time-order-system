package com.hacisimsek.rtos.inventory.service;

import com.hacisimsek.rtos.inventory.domain.MessageLog;
import com.hacisimsek.rtos.inventory.repository.MessageLogRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {
    private final MessageLogRepository repo;
    public IdempotencyService(MessageLogRepository repo) { this.repo = repo; }

    @Transactional
    public void processOnce(String messageId, Runnable action) {
        if (messageId == null || messageId.isBlank()) {
            action.run();
            return;
        }
        if (repo.existsByMessageId(messageId)) {
            return;
        }
        action.run();
        try {
            repo.save(new MessageLog(messageId));
        } catch (DataIntegrityViolationException dup) {
        }
    }
}
