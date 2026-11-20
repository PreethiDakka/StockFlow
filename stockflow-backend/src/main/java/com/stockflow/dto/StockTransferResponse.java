
package com.stockflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockTransferResponse {

    private Long transferOutMovementId;
    private Long transferInMovementId;

    private Long productId;
    private String productName;
    private String productSku;

    private Long sourceWarehouseId;
    private String sourceWarehouseName;

    private Long destinationWarehouseId;
    private String destinationWarehouseName;

    private BigDecimal quantityTransferred;
    private BigDecimal sourceRemainingQuantity;
    private BigDecimal destinationUpdatedQuantity;

    private String reference;
    private String remarks;
    private LocalDateTime transferredAt;

    public StockTransferResponse(
            Long transferOutMovementId,
            Long transferInMovementId,
            Long productId,
            String productName,
            String productSku,
            Long sourceWarehouseId,
            String sourceWarehouseName,
            Long destinationWarehouseId,
            String destinationWarehouseName,
            BigDecimal quantityTransferred,
            BigDecimal sourceRemainingQuantity,
            BigDecimal destinationUpdatedQuantity,
            String reference,
            String remarks,
            LocalDateTime transferredAt) {

        this.transferOutMovementId = transferOutMovementId;
        this.transferInMovementId = transferInMovementId;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.sourceWarehouseId = sourceWarehouseId;
        this.sourceWarehouseName = sourceWarehouseName;
        this.destinationWarehouseId = destinationWarehouseId;
        this.destinationWarehouseName = destinationWarehouseName;
        this.quantityTransferred = quantityTransferred;
        this.sourceRemainingQuantity = sourceRemainingQuantity;
        this.destinationUpdatedQuantity = destinationUpdatedQuantity;
        this.reference = reference;
        this.remarks = remarks;
        this.transferredAt = transferredAt;
    }

    public Long getTransferOutMovementId() {
        return transferOutMovementId;
    }

    public Long getTransferInMovementId() {
        return transferInMovementId;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public Long getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public String getSourceWarehouseName() {
        return sourceWarehouseName;
    }

    public Long getDestinationWarehouseId() {
        return destinationWarehouseId;
    }

    public String getDestinationWarehouseName() {
        return destinationWarehouseName;
    }

    public BigDecimal getQuantityTransferred() {
        return quantityTransferred;
    }

    public BigDecimal getSourceRemainingQuantity() {
        return sourceRemainingQuantity;
    }

    public BigDecimal getDestinationUpdatedQuantity() {
        return destinationUpdatedQuantity;
    }

    public String getReference() {
        return reference;
    }

    public String getRemarks() {
        return remarks;
    }

    public LocalDateTime getTransferredAt() {
        return transferredAt;
    }
}
