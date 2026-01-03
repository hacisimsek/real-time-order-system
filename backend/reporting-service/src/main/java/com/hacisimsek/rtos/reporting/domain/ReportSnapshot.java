package com.hacisimsek.rtos.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "report_snapshots",
        uniqueConstraints = @UniqueConstraint(name = "uq_report_snapshots_period_date",
                columnNames = {"period_type", "snapshot_date"})
)
public class ReportSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 16)
    private ReportPeriod periodType = ReportPeriod.DAILY;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_orders", nullable = false)
    private long totalOrders;

    @Column(name = "total_revenue_cents", nullable = false)
    private long totalRevenueCents;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        if (generatedAt == null) {
            generatedAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public ReportPeriod getPeriodType() {
        return periodType;
    }

    public void setPeriodType(ReportPeriod periodType) {
        this.periodType = periodType;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getTotalRevenueCents() {
        return totalRevenueCents;
    }

    public void setTotalRevenueCents(long totalRevenueCents) {
        this.totalRevenueCents = totalRevenueCents;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(OffsetDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
