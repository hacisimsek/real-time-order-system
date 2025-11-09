package com.hacisimsek.reporting.service;

import com.hacisimsek.reporting.cache.ReportCacheNames;
import com.hacisimsek.reporting.domain.ReportPeriod;
import com.hacisimsek.reporting.domain.ReportSnapshot;
import com.hacisimsek.reporting.dto.ReportListResponse;
import com.hacisimsek.reporting.dto.ReportListResponse.PageMetadata;
import com.hacisimsek.reporting.dto.ReportSummaryResponse;
import com.hacisimsek.reporting.dto.ReportTotalsResponse;
import com.hacisimsek.reporting.dto.TopCustomerResponse;
import com.hacisimsek.reporting.repository.ReportSnapshotRepository;
import com.hacisimsek.reporting.service.ReportWindowResolver.ResolvedWindow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@CacheConfig(cacheNames = {ReportCacheNames.REPORT_TOTALS, ReportCacheNames.REPORT_TOP_CUSTOMERS})
public class ReportService {

    private final ReportSnapshotRepository repository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ReportWindowResolver windowResolver;

    public ReportService(ReportSnapshotRepository repository,
                         NamedParameterJdbcTemplate jdbcTemplate,
                         ReportWindowResolver windowResolver) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.windowResolver = windowResolver;
    }

    public ReportListResponse fetchSummaries(ReportPeriod period,
                                             LocalDate startDate,
                                             LocalDate endDate,
                                             int page,
                                             int size,
                                             boolean descending) {
        ResolvedWindow window = windowResolver.resolve(period, startDate, endDate);
        Sort sort = Sort.by(descending ? Sort.Direction.DESC : Sort.Direction.ASC, "snapshotDate");
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);

        Page<ReportSnapshot> slice = repository.findSnapshots(
                window.period(), window.start(), window.end(), pageable);

        List<ReportSummaryResponse> items = slice.stream()
                .map(this::toResponse)
                .toList();

        ReportSnapshotRepository.SnapshotTotals totals = repository.computeTotals(
                window.period(), window.start(), window.end());
        ReportTotalsResponse totalsResponse = toTotals(window, totals);

        PageMetadata meta = new PageMetadata(
                slice.getNumber(),
                slice.getSize(),
                slice.getTotalElements(),
                slice.getTotalPages());

        return new ReportListResponse(items, meta, totalsResponse);
    }

    public List<ReportSummaryResponse> fetchSummaries(ReportPeriod period,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        ResolvedWindow window = windowResolver.resolve(period, startDate, endDate);
        List<ReportSnapshot> snapshots = repository.findSnapshots(window.period(), window.start(), window.end());
        return snapshots.stream().map(this::toResponse).toList();
    }

    @Cacheable(cacheNames = ReportCacheNames.REPORT_TOTALS,
            key = "@reportWindowResolver.cacheKey(#period, #startDate, #endDate)")
    public ReportTotalsResponse fetchTotals(ReportPeriod period,
                                            LocalDate startDate,
                                            LocalDate endDate) {
        ResolvedWindow window = windowResolver.resolve(period, startDate, endDate);
        ReportSnapshotRepository.SnapshotTotals totals = repository.computeTotals(
                window.period(), window.start(), window.end());
        return toTotals(window, totals);
    }

    @Cacheable(cacheNames = ReportCacheNames.REPORT_TOP_CUSTOMERS,
            key = "@reportWindowResolver.cacheKey(#period, #startDate, #endDate) + ':' + (#limit > 0 ? #limit : 5)")
    public List<TopCustomerResponse> topCustomers(ReportPeriod period,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  int limit) {
        ResolvedWindow window = windowResolver.resolve(period, startDate, endDate);
        int resolvedLimit = limit > 0 ? limit : 5;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("start", window.start().atStartOfDay().atOffset(ZoneOffset.UTC));
        params.addValue("end", window.end().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
        params.addValue("limit", resolvedLimit);

        RowMapper<TopCustomerResponse> mapper = (rs, rowNum) -> {
            long revenueCents = rs.getLong("total_revenue_cents");
            BigDecimal revenue = BigDecimal.valueOf(revenueCents, 2);
            return new TopCustomerResponse(
                    rs.getString("customer_id"),
                    rs.getLong("total_orders"),
                    revenueCents,
                    revenue
            );
        };

        String sql = """
                SELECT customer_id,
                       COUNT(*) AS total_orders,
                       COALESCE(SUM(amount_cents), 0) AS total_revenue_cents
                  FROM orders
                 WHERE created_at >= :start AND created_at < :end
                 GROUP BY customer_id
                 ORDER BY total_revenue_cents DESC
                 LIMIT :limit
                """;

        return jdbcTemplate.query(sql, params, mapper);
    }

    private ReportTotalsResponse toTotals(ResolvedWindow window,
                                          ReportSnapshotRepository.SnapshotTotals totals) {
        BigDecimal revenue = BigDecimal.valueOf(totals.getTotalRevenueCents(), 2);
        return new ReportTotalsResponse(
                window.period(),
                window.start(),
                window.end(),
                totals.getTotalOrders(),
                totals.getTotalRevenueCents(),
                revenue
        );
    }

    private ReportSummaryResponse toResponse(ReportSnapshot snapshot) {
        BigDecimal revenue = BigDecimal.valueOf(snapshot.getTotalRevenueCents(), 2);
        return new ReportSummaryResponse(
                snapshot.getPeriodType(),
                snapshot.getSnapshotDate(),
                snapshot.getTotalOrders(),
                snapshot.getTotalRevenueCents(),
                revenue
        );
    }
}
