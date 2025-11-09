package com.hacisimsek.reporting.service;

import com.hacisimsek.reporting.cache.ReportCacheNames;
import com.hacisimsek.reporting.domain.ReportPeriod;
import com.hacisimsek.reporting.domain.ReportSnapshot;
import com.hacisimsek.reporting.repository.ReportSnapshotRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReportSnapshotRefreshService {

    private static final EnumSet<ReportPeriod> PERIODS = EnumSet.allOf(ReportPeriod.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ReportSnapshotRepository repository;

    public ReportSnapshotRefreshService(NamedParameterJdbcTemplate jdbcTemplate,
                                        ReportSnapshotRepository repository) {
        this.jdbcTemplate = jdbcTemplate;
        this.repository = repository;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = ReportCacheNames.REPORT_TOTALS, allEntries = true),
            @CacheEvict(cacheNames = ReportCacheNames.REPORT_TOP_CUSTOMERS, allEntries = true)
    })
    public void refresh(LocalDate startDate, LocalDate endDate) {
        LocalDate end = Optional.ofNullable(endDate).orElse(LocalDate.now());
        LocalDate start = Optional.ofNullable(startDate).orElse(end.minusMonths(12).withDayOfMonth(1));

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }

        for (ReportPeriod period : PERIODS) {
            refreshPeriod(period, start, end);
        }
    }

    private void refreshPeriod(ReportPeriod period, LocalDate start, LocalDate end) {
        LocalDate normalizedStart = switch (period) {
            case DAILY -> start;
            case WEEKLY -> start.with(DayOfWeek.MONDAY);
            case MONTHLY -> start.withDayOfMonth(1);
        };
        LocalDate normalizedEnd = switch (period) {
            case DAILY -> end;
            case WEEKLY -> end.with(DayOfWeek.MONDAY);
            case MONTHLY -> end.withDayOfMonth(1);
        };
        if (normalizedEnd.isBefore(normalizedStart)) {
            normalizedEnd = normalizedStart;
        }
        repository.deleteByPeriodTypeAndSnapshotDateBetween(period, normalizedStart, normalizedEnd);

        MapSqlParameterSource params = new MapSqlParameterSource();
        LocalDate startDate = start;
        LocalDate endExclusive = end.plusDays(1);
        params.addValue("start", startDate);
        params.addValue("end", endExclusive);

        String sql = switch (period) {
            case DAILY -> """
                    SELECT bucket_date AS bucket,
                           total_orders,
                           total_revenue_cents
                      FROM report_order_rollup_daily
                     WHERE bucket_date >= :start
                       AND bucket_date < :end
                     ORDER BY bucket
                    """;
            case WEEKLY -> """
                    SELECT date_trunc('week', bucket_date::timestamp AT TIME ZONE 'UTC')::date AS bucket,
                           SUM(total_orders) AS total_orders,
                           SUM(total_revenue_cents) AS total_revenue_cents
                      FROM report_order_rollup_daily
                     WHERE bucket_date >= :start
                       AND bucket_date < :end
                     GROUP BY bucket
                     ORDER BY bucket
                    """;
            case MONTHLY -> """
                    SELECT date_trunc('month', bucket_date::timestamp AT TIME ZONE 'UTC')::date AS bucket,
                           SUM(total_orders) AS total_orders,
                           SUM(total_revenue_cents) AS total_revenue_cents
                      FROM report_order_rollup_daily
                     WHERE bucket_date >= :start
                       AND bucket_date < :end
                     GROUP BY bucket
                     ORDER BY bucket
                    """;
        };

        RowMapper<SnapshotRow> mapper = (rs, rowNum) -> new SnapshotRow(
                rs.getDate("bucket").toLocalDate(),
                rs.getLong("total_orders"),
                rs.getLong("total_revenue_cents"));

        List<SnapshotRow> rows = jdbcTemplate.query(sql, params, mapper);

        for (SnapshotRow row : rows) {
            ReportSnapshot snapshot = repository
                    .findByPeriodTypeAndSnapshotDate(period, row.bucket())
                    .orElseGet(() -> {
                        ReportSnapshot s = new ReportSnapshot();
                        s.setPeriodType(period);
                        s.setSnapshotDate(row.bucket());
                        return s;
                    });
            snapshot.setTotalOrders(row.totalOrders());
            snapshot.setTotalRevenueCents(row.totalRevenueCents());
            snapshot.setGeneratedAt(OffsetDateTime.now(ZoneOffset.UTC));
            repository.save(snapshot);
        }
    }

    private record SnapshotRow(LocalDate bucket, long totalOrders, long totalRevenueCents) {}
}
