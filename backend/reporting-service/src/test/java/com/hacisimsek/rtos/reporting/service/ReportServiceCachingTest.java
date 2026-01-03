package com.hacisimsek.rtos.reporting.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hacisimsek.rtos.reporting.cache.ReportCacheNames;
import com.hacisimsek.rtos.reporting.config.CacheConfig;
import com.hacisimsek.rtos.reporting.config.CacheProps;
import com.hacisimsek.rtos.reporting.domain.ReportPeriod;
import com.hacisimsek.rtos.reporting.dto.TopCustomerResponse;
import com.hacisimsek.rtos.reporting.repository.ReportSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        ReportService.class,
        CacheConfig.class,
        ReportWindowResolver.class
})
class ReportServiceCachingTest {

    @Autowired
    private ReportService service;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private ReportSnapshotRepository repository;

    @MockBean
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearCaches() {
        Objects.requireNonNull(cacheManager.getCache(ReportCacheNames.REPORT_TOTALS)).clear();
        Objects.requireNonNull(cacheManager.getCache(ReportCacheNames.REPORT_TOP_CUSTOMERS)).clear();
    }

    @Test
    void fetchTotalsServedFromCacheOnSubsequentCalls() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        ReportSnapshotRepository.SnapshotTotals totals = new ReportSnapshotRepository.SnapshotTotals() {
            @Override
            public long getTotalOrders() {
                return 10;
            }

            @Override
            public long getTotalRevenueCents() {
                return 25_000;
            }
        };

        when(repository.computeTotals(ReportPeriod.MONTHLY, start, end)).thenReturn(totals);

        service.fetchTotals(ReportPeriod.MONTHLY, start, end);
        service.fetchTotals(ReportPeriod.MONTHLY, start, end);

        verify(repository, times(1)).computeTotals(ReportPeriod.MONTHLY, start, end);
    }

    @Test
    void topCustomersCacheKeyIncludesLimit() {
        LocalDate start = LocalDate.of(2025, 2, 1);
        LocalDate end = LocalDate.of(2025, 2, 7);

        List<TopCustomerResponse> response = List.of(
                new TopCustomerResponse("C-1", 5, 12_500, new BigDecimal("125.00")));

        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(response);

        service.topCustomers(ReportPeriod.WEEKLY, start, end, 10);
        service.topCustomers(ReportPeriod.WEEKLY, start, end, 10);
        service.topCustomers(ReportPeriod.WEEKLY, start, end, 5);

        verify(jdbcTemplate, times(2))
                .query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class));
    }
}
