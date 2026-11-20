
package com.stockflow.controller;

import com.stockflow.dto.PurchaseOrderRequest;
import com.stockflow.dto.PurchaseOrderResponse;
import com.stockflow.service.PurchaseOrderService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(
            PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    // CREATE PURCHASE ORDER
    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequest request) {

        PurchaseOrderResponse response =
                purchaseOrderService.createPurchaseOrder(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET ALL PURCHASE ORDERS
    @GetMapping
    public ResponseEntity<Page<PurchaseOrderResponse>> getAllPurchaseOrders(
            Pageable pageable) {

        return ResponseEntity.ok(
                purchaseOrderService.getAllPurchaseOrders(pageable));
    }

    // GET PURCHASE ORDER BY ID
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> getPurchaseOrderById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                purchaseOrderService.getPurchaseOrderById(id));
    }

    // MARK PURCHASE ORDER AS ORDERED
    @PatchMapping("/{id}/order")
    public ResponseEntity<PurchaseOrderResponse> markAsOrdered(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                purchaseOrderService.markAsOrdered(id));
    }

    // CANCEL PURCHASE ORDER
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrderResponse> cancelPurchaseOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                purchaseOrderService.cancelPurchaseOrder(id));
    }

    // RECEIVE PURCHASE ORDER
    @PatchMapping("/{id}/receive")
    public ResponseEntity<PurchaseOrderResponse> receivePurchaseOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                purchaseOrderService.receivePurchaseOrder(id));
    }
}
