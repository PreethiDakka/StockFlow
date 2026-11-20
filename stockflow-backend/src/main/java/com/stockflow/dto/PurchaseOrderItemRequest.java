
package com.stockflow.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PurchaseOrderItemRequest {

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be greater than zero")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(
            value = "0.001",
            message = "Quantity must be greater than zero"
    )
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(
            value = "0.01",
            message = "Unit price must be greater than zero"
    )
    private BigDecimal unitPrice;

    public PurchaseOrderItemRequest() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
