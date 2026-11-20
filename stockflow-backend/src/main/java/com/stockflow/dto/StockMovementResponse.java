
package com.stockflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockMovementResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long warehouseId;
    private String warehouseName;
    private String movementType;
    private BigDecimal quantity;
    private String reference;
    private String remarks;
    private LocalDateTime createdAt;

    public StockMovementResponse(
            Long id,
            Long productId,
            String productName,
            String productSku,
            Long warehouseId,
            String warehouseName,
            String movementType,
            BigDecimal quantity,
            String reference,
            String remarks,
            LocalDateTime createdAt) {

        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.movementType = movementType;
        this.quantity = quantity;
        this.reference = reference;
        this.remarks = remarks;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductSku() { return productSku; }
    public Long getWarehouseId() { return warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public String getMovementType() { return movementType; }
    public BigDecimal getQuantity() { return quantity; }
    public String getReference() { return reference; }
    public String getRemarks() { return remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
