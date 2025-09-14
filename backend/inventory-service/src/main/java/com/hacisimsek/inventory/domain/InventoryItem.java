package com.hacisimsek.inventory.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory")
public class InventoryItem {
    @Id
    @Column(length = 64, nullable = false)
    private String sku;

    @Column(nullable = false)
    private int availableQty = 0;

    @Column(nullable = false)
    private int reservedQty = 0;

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate @PrePersist
    void touch() { this.updatedAt = OffsetDateTime.now(); }

    // getters/setters
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public int getAvailableQty() { return availableQty; }
    public void setAvailableQty(int availableQty) { this.availableQty = availableQty; }
    public int getReservedQty() { return reservedQty; }
    public void setReservedQty(int reservedQty) { this.reservedQty = reservedQty; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
