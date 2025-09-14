package com.hacisimsek.inventory.web;

import com.hacisimsek.inventory.dto.AdjustRequest;
import com.hacisimsek.inventory.dto.InventoryResponse;
import com.hacisimsek.inventory.dto.ReserveRequest;
import com.hacisimsek.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService service;
    public InventoryController(InventoryService service) { this.service = service; }

    @GetMapping("/{sku}")
    public ResponseEntity<InventoryResponse> get(@PathVariable String sku) {
        return ResponseEntity.ok(service.get(sku));
    }

    @PutMapping("/{sku}/adjust")
    public ResponseEntity<InventoryResponse> adjust(@PathVariable String sku, @Valid @RequestBody AdjustRequest req) {
        return ResponseEntity.ok(service.adjust(sku, req));
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryResponse> reserve(@Valid @RequestBody ReserveRequest req) {
        return ResponseEntity.ok(service.reserve(req));
    }

    @PostMapping("/release")
    public ResponseEntity<InventoryResponse> release(@Valid @RequestBody ReserveRequest req) {
        return ResponseEntity.ok(service.release(req));
    }

    @PostMapping("/consume")
    public ResponseEntity<InventoryResponse> consume(@Valid @RequestBody ReserveRequest req) {
        return ResponseEntity.ok(service.consume(req.sku(), req.qty()));
    }
}
