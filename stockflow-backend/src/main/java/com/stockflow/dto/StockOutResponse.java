
package com.stockflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockOutResponse {

    private Long movementId;
    private Long productId;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private String movementType;
    private BigDecimal quantityRemoved;
    private BigDecimal remainingQuantity;
    private String reference;
    private String remarks;
    private LocalDateTime createdAt;

    public StockOutResponse(
            Long movementId,
            Long productId,
            String productName,
            Long warehouseId,
            String warehouseName,
            String movementType,
            BigDecimal quantityRemoved,
            BigDecimal remainingQuantity,
            String reference,
            String remarks,
            LocalDateTime createdAt) {

        this.movementId = movementId;
        this.productId = productId;
        this.productName = productName;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.movementType = movementType;
        this.quantityRemoved = quantityRemoved;
        this.remainingQuantity = remainingQuantity;
        this.reference = reference;
        this.remarks = remarks;
        this.createdAt = createdAt;
    }

    public Long getMovementId() { return movementId; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Long getWarehouseId() { return warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public String getMovementType() { return movementType; }
    public BigDecimal getQuantityRemoved() { return quantityRemoved; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public String getReference() { return reference; }
    public String getRemarks() { return remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
