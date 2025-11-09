package com.hacisimsek.reporting.repository;

import com.hacisimsek.reporting.domain.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageLogRepository extends JpaRepository<MessageLog, String> {
}
