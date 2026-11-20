
package com.stockflow.controller;

import com.stockflow.dto.WarehouseRequest;
import com.stockflow.dto.WarehouseResponse;
import com.stockflow.service.WarehouseService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    // Create a warehouse
    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @Valid @RequestBody WarehouseRequest request) {

        WarehouseResponse savedWarehouse =
                warehouseService.createWarehouse(request);

        return ResponseEntity.status(201).body(savedWarehouse);
    }

    // Get all warehouses
    @GetMapping
    public ResponseEntity<List<WarehouseResponse>> getAllWarehouses() {

        return ResponseEntity.ok(
                warehouseService.getAllWarehouses()
        );
    }

    // Get warehouse by ID
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponse> getWarehouseById(
            @PathVariable Long id) {

        return warehouseService.getWarehouseById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update warehouse
    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseRequest request) {

        return warehouseService.updateWarehouse(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Deactivate warehouse
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateWarehouse(
            @PathVariable Long id) {

        boolean deactivated =
                warehouseService.deactivateWarehouse(id);

        if (!deactivated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    // Activate warehouse
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateWarehouse(
            @PathVariable Long id) {

        boolean activated =
                warehouseService.activateWarehouse(id);

        if (!activated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
