package com.hacisimsek.rtos.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "report_order_rollup_daily")
public class DailyOrderRollup {

    @Id
    @Column(name = "bucket_date", nullable = false)
    private LocalDate bucketDate;

    @Column(name = "total_orders", nullable = false)
    private long totalOrders;

    @Column(name = "total_revenue_cents", nullable = false)
    private long totalRevenueCents;

    public DailyOrderRollup() {
    }

    public DailyOrderRollup(LocalDate bucketDate, long totalOrders, long totalRevenueCents) {
        this.bucketDate = bucketDate;
        this.totalOrders = totalOrders;
        this.totalRevenueCents = totalRevenueCents;
    }

    public LocalDate getBucketDate() {
        return bucketDate;
    }

    public void setBucketDate(LocalDate bucketDate) {
        this.bucketDate = bucketDate;
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
}
