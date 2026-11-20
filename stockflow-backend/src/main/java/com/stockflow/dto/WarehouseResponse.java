package com.stockflow.dto;

import java.time.LocalDateTime;

public record WarehouseResponse(
        Long id,
        String name,
        String code,
        String address,
        String city,
        String state,
        String country,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}