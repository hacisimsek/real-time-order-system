package com.hacisimsek.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hacisimsek.reporting.domain.ReportPeriod;
import com.hacisimsek.reporting.domain.ReportSnapshot;
import com.hacisimsek.reporting.dto.ReportListResponse;
import com.hacisimsek.reporting.dto.ReportSummaryResponse;
import com.hacisimsek.reporting.dto.TopCustomerResponse;
import com.hacisimsek.reporting.repository.ReportSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportSnapshotRepository repository;

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private ReportWindowResolver windowResolver;

    private ReportService service;

    @BeforeEach
    void setUp() {
        windowResolver = new ReportWindowResolver();
        service = new ReportService(repository, jdbcTemplate, windowResolver);
    }

    @Test
    void fetchSummariesMapsEntityToDto() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 7);

        ReportSnapshot snapshot = new ReportSnapshot();
        snapshot.setPeriodType(ReportPeriod.DAILY);
        snapshot.setSnapshotDate(LocalDate.of(2025, 1, 3));
        snapshot.setTotalOrders(42);
        snapshot.setTotalRevenueCents(123_456);

        when(repository.findSnapshots(ReportPeriod.DAILY, start, end))
                .thenReturn(List.of(snapshot));

        List<ReportSummaryResponse> responses = service.fetchSummaries(ReportPeriod.DAILY, start, end);

        assertThat(responses).hasSize(1);
        ReportSummaryResponse response = responses.get(0);
        assertThat(response.period()).isEqualTo(ReportPeriod.DAILY);
        assertThat(response.snapshotDate()).isEqualTo(LocalDate.of(2025, 1, 3));
        assertThat(response.totalOrders()).isEqualTo(42);
        assertThat(response.totalRevenueCents()).isEqualTo(123_456);
        assertThat(response.totalRevenue().toPlainString()).isEqualTo("1234.56");

        verify(repository).findSnapshots(ReportPeriod.DAILY, start, end);
    }

    @Test
    void throwsWhenStartAfterEnd() {
        LocalDate start = LocalDate.of(2025, 1, 10);
        LocalDate end = LocalDate.of(2025, 1, 5);

        assertThatThrownBy(() -> service.fetchSummaries(ReportPeriod.DAILY, start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startDate");

        verifyNoInteractions(repository);
    }

    @Test
    void usesDefaultsWhenDatesMissing() {
        when(repository.findSnapshots(
                ArgumentMatchers.eq(ReportPeriod.WEEKLY),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()))
                .thenReturn(List.of());

        List<ReportSummaryResponse> responses = service.fetchSummaries(ReportPeriod.WEEKLY, null, null);

        assertThat(responses).isEmpty();
        verify(repository).findSnapshots(ArgumentMatchers.eq(ReportPeriod.WEEKLY),
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void fetchSummariesPagedReturnsMetadataAndTotals() {
        LocalDate start = LocalDate.of(2025, 2, 1);
        LocalDate end = LocalDate.of(2025, 2, 7);

        ReportSnapshot snapshot = new ReportSnapshot();
        snapshot.setPeriodType(ReportPeriod.DAILY);
        snapshot.setSnapshotDate(LocalDate.of(2025, 2, 2));
        snapshot.setTotalOrders(20);
        snapshot.setTotalRevenueCents(55_000);

        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "snapshotDate"));
        Page<ReportSnapshot> page = new PageImpl<>(List.of(snapshot), pageable, 1);

        when(repository.findSnapshots(ReportPeriod.DAILY, start, end, pageable)).thenReturn(page);

        ReportSnapshotRepository.SnapshotTotals totals = new ReportSnapshotRepository.SnapshotTotals() {
            @Override
            public long getTotalOrders() {
                return 20;
            }

            @Override
            public long getTotalRevenueCents() {
                return 55_000;
            }
        };

        when(repository.computeTotals(ReportPeriod.DAILY, start, end)).thenReturn(totals);

        ReportListResponse response = service.fetchSummaries(ReportPeriod.DAILY, start, end, 0, 10, false);

        assertThat(response.items()).hasSize(1);
        assertThat(response.page().totalElements()).isEqualTo(1);
        assertThat(response.page().page()).isEqualTo(0);
        assertThat(response.totals().totalOrders()).isEqualTo(20);
        assertThat(response.totals().totalRevenue().toPlainString()).isEqualTo("550.00");

        verify(repository).findSnapshots(ReportPeriod.DAILY, start, end, pageable);
        verify(repository).computeTotals(ReportPeriod.DAILY, start, end);
    }

    @Test
    void topCustomersFallsBackToDefaultLimit() {
        TopCustomerResponse customer = new TopCustomerResponse("C-1", 5, 12_500, new BigDecimal("125.00"));
        when(jdbcTemplate.query(ArgumentMatchers.anyString(), ArgumentMatchers.any(MapSqlParameterSource.class), ArgumentMatchers.any(RowMapper.class)))
                .thenReturn(List.of(customer));

        List<TopCustomerResponse> result = service.topCustomers(ReportPeriod.MONTHLY, null, null, 0);

        assertThat(result).hasSize(1).first()
                .extracting(TopCustomerResponse::customerId)
                .isEqualTo("C-1");
        verify(jdbcTemplate).query(ArgumentMatchers.anyString(), ArgumentMatchers.any(MapSqlParameterSource.class), ArgumentMatchers.any(RowMapper.class));
    }
}
