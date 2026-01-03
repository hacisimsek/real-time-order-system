package com.hacisimsek.rtos.reporting.repository;

import com.hacisimsek.rtos.reporting.domain.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageLogRepository extends JpaRepository<MessageLog, String> {
}
