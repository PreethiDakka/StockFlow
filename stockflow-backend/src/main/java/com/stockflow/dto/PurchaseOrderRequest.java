
package com.stockflow.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class PurchaseOrderRequest {

    @NotNull(message = "Supplier is required")
    @Positive(message = "Supplier ID must be positive")
    private Long supplierId;

    @NotNull(message = "Warehouse is required")
    @Positive(message = "Warehouse ID must be positive")
    private Long warehouseId;

    @FutureOrPresent(message = "Expected delivery date cannot be in the past")
    private LocalDate expectedDeliveryDate;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    @NotEmpty(message = "A purchase order must contain at least one item")
    @Size(max = 100, message = "A purchase order cannot contain more than 100 items")
    @Valid
    private List<PurchaseOrderItemRequest> items;

    public PurchaseOrderRequest() {
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? null : notes.trim();
    }

    public List<PurchaseOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItemRequest> items) {
        this.items = items;
    }
}
