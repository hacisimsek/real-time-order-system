package com.hacisimsek.rtos.reporting.repository;

import com.hacisimsek.rtos.reporting.domain.DailyOrderRollup;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyOrderRollupRepository extends JpaRepository<DailyOrderRollup, LocalDate> {
    Optional<DailyOrderRollup> findByBucketDate(LocalDate bucketDate);
}
