package com.hacisimsek.reporting.repository;

import com.hacisimsek.reporting.domain.ReportPeriod;
import com.hacisimsek.reporting.domain.ReportSnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportSnapshotRepository extends JpaRepository<ReportSnapshot, Long> {

    @Query("""
            select r from ReportSnapshot r
            where r.periodType = :period
              and r.snapshotDate >= :startDate
              and r.snapshotDate <= :endDate
            order by r.snapshotDate asc
            """)
    List<ReportSnapshot> findSnapshots(
            @Param("period") ReportPeriod period,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<ReportSnapshot> findByPeriodTypeAndSnapshotDate(ReportPeriod period, LocalDate snapshotDate);

    void deleteByPeriodTypeAndSnapshotDateBetween(ReportPeriod period, LocalDate startDate, LocalDate endDate);

    @Query(value = """
            select r from ReportSnapshot r
            where r.periodType = :period
              and r.snapshotDate >= :startDate
              and r.snapshotDate <= :endDate
            """,
            countQuery = """
            select count(r) from ReportSnapshot r
            where r.periodType = :period
              and r.snapshotDate >= :startDate
              and r.snapshotDate <= :endDate
            """)
    Page<ReportSnapshot> findSnapshots(
            @Param("period") ReportPeriod period,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    interface SnapshotTotals {
        long getTotalOrders();
        long getTotalRevenueCents();
    }

    @Query("""
            select coalesce(sum(r.totalOrders),0) as totalOrders,
                   coalesce(sum(r.totalRevenueCents),0) as totalRevenueCents
            from ReportSnapshot r
            where r.periodType = :period
              and r.snapshotDate >= :startDate
              and r.snapshotDate <= :endDate
            """)
    SnapshotTotals computeTotals(
            @Param("period") ReportPeriod period,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
