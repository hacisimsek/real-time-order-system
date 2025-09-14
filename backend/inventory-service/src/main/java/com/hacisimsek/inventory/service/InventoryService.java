package com.hacisimsek.inventory.service;

import com.hacisimsek.inventory.domain.InventoryItem;
import com.hacisimsek.inventory.dto.AdjustRequest;
import com.hacisimsek.inventory.dto.InventoryResponse;
import com.hacisimsek.inventory.dto.ReserveRequest;
import com.hacisimsek.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryRepository repo;
    public InventoryService(InventoryRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public InventoryResponse get(String sku) {
        var item = repo.findById(sku).orElseThrow(() -> new IllegalArgumentException("SKU not found"));
        return toDto(item);
    }

    @Transactional
    public InventoryResponse adjust(String sku, AdjustRequest req) {
        int delta = req.delta();
        var item = repo.findById(sku).orElseGet(() -> { var i = new InventoryItem(); i.setSku(sku); return i; });
        int newAvail = item.getAvailableQty() + delta;
        if (newAvail < 0) throw new IllegalArgumentException("Insufficient available quantity");
        item.setAvailableQty(newAvail);
        var saved = repo.save(item);
        return toDto(saved);
    }

    @Transactional
    public InventoryResponse reserve(ReserveRequest req) {
        var item = repo.findById(req.sku()).orElseThrow(() -> new IllegalArgumentException("SKU not found"));
        if (item.getAvailableQty() < req.qty()) throw new IllegalArgumentException("Not enough stock to reserve");
        item.setAvailableQty(item.getAvailableQty() - req.qty());
        item.setReservedQty(item.getReservedQty() + req.qty());
        var saved = repo.save(item);
        return toDto(saved);
    }

    @Transactional
    public InventoryResponse release(ReserveRequest req) {
        var item = repo.findById(req.sku()).orElseThrow(() -> new IllegalArgumentException("SKU not found"));
        if (item.getReservedQty() < req.qty()) throw new IllegalArgumentException("Not enough reserved to release");
        item.setReservedQty(item.getReservedQty() - req.qty());
        item.setAvailableQty(item.getAvailableQty() + req.qty());
        var saved = repo.save(item);
        return toDto(saved);
    }

    @Transactional
    public InventoryResponse consume(String sku, int qty) {
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");
        var item = repo.findById(sku).orElseThrow(() -> new IllegalArgumentException("SKU not found"));
        if (item.getReservedQty() < qty) {
            throw new IllegalArgumentException("Reserved not enough to consume");
        }
        item.setReservedQty(item.getReservedQty() - qty);
        var saved = repo.save(item);
        return toDto(saved);
    }

    private InventoryResponse toDto(InventoryItem i) {
        return new InventoryResponse(i.getSku(), i.getAvailableQty(), i.getReservedQty(), i.getUpdatedAt());
    }
}
