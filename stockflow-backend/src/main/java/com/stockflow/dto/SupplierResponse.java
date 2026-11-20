package com.stockflow.dto;

import java.time.LocalDateTime;

public record SupplierResponse(
        Long id,
        String name,
        String code,
        String contactPerson,
        String email,
        String phone,
        String address,
        String city,
        String state,
        String country,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
