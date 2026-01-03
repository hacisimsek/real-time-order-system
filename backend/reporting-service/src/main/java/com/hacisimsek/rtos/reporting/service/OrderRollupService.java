package com.hacisimsek.rtos.reporting.service;

import com.hacisimsek.rtos.reporting.domain.DailyOrderRollup;
import com.hacisimsek.rtos.reporting.domain.ReportPeriod;
import com.hacisimsek.rtos.reporting.domain.ReportSnapshot;
import com.hacisimsek.rtos.reporting.repository.DailyOrderRollupRepository;
import com.hacisimsek.rtos.reporting.repository.ReportSnapshotRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderRollupService {

    private final DailyOrderRollupRepository rollupRepository;
    private final ReportSnapshotRepository snapshotRepository;

    public OrderRollupService(DailyOrderRollupRepository rollupRepository,
                              ReportSnapshotRepository snapshotRepository) {
        this.rollupRepository = rollupRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public void recordOrder(LocalDate orderDate, long amountCents) {
        LocalDate bucket = orderDate;
        DailyOrderRollup rollup = rollupRepository.findByBucketDate(bucket)
                .orElseGet(() -> new DailyOrderRollup(bucket, 0, 0));
        rollup.setTotalOrders(rollup.getTotalOrders() + 1);
        rollup.setTotalRevenueCents(rollup.getTotalRevenueCents() + amountCents);
        rollupRepository.save(rollup);

        upsertSnapshot(ReportPeriod.DAILY, bucket, 1, amountCents);

        LocalDate weekBucket = bucket.with(DayOfWeek.MONDAY);
        upsertSnapshot(ReportPeriod.WEEKLY, weekBucket, 1, amountCents);

        LocalDate monthBucket = bucket.withDayOfMonth(1);
        upsertSnapshot(ReportPeriod.MONTHLY, monthBucket, 1, amountCents);
    }

    private void upsertSnapshot(ReportPeriod period, LocalDate bucket, long orderDelta, long revenueDelta) {
        ReportSnapshot snapshot = snapshotRepository.findByPeriodTypeAndSnapshotDate(period, bucket)
                .orElseGet(() -> {
                    ReportSnapshot s = new ReportSnapshot();
                    s.setPeriodType(period);
                    s.setSnapshotDate(bucket);
                    s.setTotalOrders(0);
                    s.setTotalRevenueCents(0);
                    return s;
                });

        snapshot.setTotalOrders(snapshot.getTotalOrders() + orderDelta);
        snapshot.setTotalRevenueCents(snapshot.getTotalRevenueCents() + revenueDelta);
        snapshot.setGeneratedAt(OffsetDateTime.now(ZoneOffset.UTC));
        snapshotRepository.save(snapshot);
    }
}
