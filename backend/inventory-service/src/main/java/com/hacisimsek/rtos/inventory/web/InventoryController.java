package com.hacisimsek.rtos.inventory.web;

import com.hacisimsek.rtos.inventory.dto.AdjustRequest;
import com.hacisimsek.rtos.inventory.dto.InventoryResponse;
import com.hacisimsek.rtos.inventory.dto.ReserveRequest;
import com.hacisimsek.rtos.inventory.service.InventoryService;
import com.hacisimsek.rtos.security.Roles;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService service;
    public InventoryController(InventoryService service) { this.service = service; }

    @GetMapping("/{sku}")
    @PreAuthorize("hasAnyAuthority('" + Roles.INVENTORY_READ + "','" + Roles.INVENTORY_WRITE + "')")
    public ResponseEntity<InventoryResponse> get(@PathVariable String sku) {
        return ResponseEntity.ok(service.get(sku));
    }

    @PutMapping("/{sku}/adjust")
    @PreAuthorize("hasAuthority('" + Roles.INVENTORY_WRITE + "')")
    public ResponseEntity<InventoryResponse> adjust(@PathVariable String sku, @Valid @RequestBody AdjustRequest req) {
        return ResponseEntity.ok(service.adjust(sku, req));
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasAuthority('" + Roles.INVENTORY_WRITE + "')")
    public ResponseEntity<InventoryResponse> reserve(@Valid @RequestBody ReserveRequest req) {
        return ResponseEntity.ok(service.reserve(req));
    }

    @PostMapping("/release")
    @PreAuthorize("hasAuthority('" + Roles.INVENTORY_WRITE + "')")
    public ResponseEntity<InventoryResponse> release(@Valid @RequestBody ReserveRequest req) {
        return ResponseEntity.ok(service.release(req));
    }

    @PostMapping("/consume")
    @PreAuthorize("hasAuthority('" + Roles.INVENTORY_WRITE + "')")
    public ResponseEntity<InventoryResponse> consume(@Valid @RequestBody ReserveRequest req) {
        return ResponseEntity.ok(service.consume(req.sku(), req.qty()));
    }
}
