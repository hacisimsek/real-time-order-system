package com.hacisimsek.rtos.inventory.dto;

import java.time.OffsetDateTime;

public record InventoryResponse(String sku, int availableQty, int reservedQty, OffsetDateTime updatedAt) {}
