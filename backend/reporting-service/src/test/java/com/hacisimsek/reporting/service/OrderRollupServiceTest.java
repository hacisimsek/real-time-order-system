package com.hacisimsek.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hacisimsek.reporting.domain.DailyOrderRollup;
import com.hacisimsek.reporting.domain.ReportPeriod;
import com.hacisimsek.reporting.domain.ReportSnapshot;
import com.hacisimsek.reporting.repository.DailyOrderRollupRepository;
import com.hacisimsek.reporting.repository.ReportSnapshotRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderRollupServiceTest {

    @Mock
    private DailyOrderRollupRepository rollupRepository;

    @Mock
    private ReportSnapshotRepository snapshotRepository;

    private OrderRollupService service;

    @BeforeEach
    void setUp() {
        service = new OrderRollupService(rollupRepository, snapshotRepository);
    }

    @Test
    void recordOrderCreatesAndUpdatesSnapshots() {
        LocalDate date = LocalDate.of(2025, 2, 3);

        when(rollupRepository.findByBucketDate(date)).thenReturn(Optional.empty());
        when(snapshotRepository.findByPeriodTypeAndSnapshotDate(ReportPeriod.DAILY, date)).thenReturn(Optional.empty());
        LocalDate weekBucket = date.with(java.time.DayOfWeek.MONDAY);
        when(snapshotRepository.findByPeriodTypeAndSnapshotDate(ReportPeriod.WEEKLY, weekBucket)).thenReturn(Optional.empty());
        LocalDate monthBucket = date.withDayOfMonth(1);
        when(snapshotRepository.findByPeriodTypeAndSnapshotDate(ReportPeriod.MONTHLY, monthBucket)).thenReturn(Optional.empty());

        service.recordOrder(date, 123_45);

        ArgumentCaptor<DailyOrderRollup> rollupCaptor = ArgumentCaptor.forClass(DailyOrderRollup.class);
        verify(rollupRepository).save(rollupCaptor.capture());
        assertThat(rollupCaptor.getValue().getTotalOrders()).isEqualTo(1);
        assertThat(rollupCaptor.getValue().getTotalRevenueCents()).isEqualTo(123_45);

        ArgumentCaptor<ReportSnapshot> snapshotCaptor = ArgumentCaptor.forClass(ReportSnapshot.class);
        verify(snapshotRepository, times(3)).save(snapshotCaptor.capture());
    }
}
