package com.stockflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Integer reorderLevel;
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InventoryResponse() {
    }

    public InventoryResponse(
            Long id,
            Long productId,
            String productName,
            String productSku,
            Integer reorderLevel,
            Long warehouseId,
            String warehouseName,
            BigDecimal quantity,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.reorderLevel = reorderLevel;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductSku() { return productSku; }
    public Integer getReorderLevel() { return reorderLevel; }
    public Long getWarehouseId() { return warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public BigDecimal getQuantity() { return quantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
