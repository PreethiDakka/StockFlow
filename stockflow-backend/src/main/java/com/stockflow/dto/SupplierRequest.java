package com.stockflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank(message = "Supplier name is required")
        @Size(max = 150, message = "Supplier name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Supplier code is required")
        @Size(max = 50, message = "Supplier code must not exceed 50 characters")
        String code,

        @Size(max = 150, message = "Contact person must not exceed 150 characters")
        String contactPerson,

        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 100, message = "State must not exceed 100 characters")
        String state,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country
) {
}
