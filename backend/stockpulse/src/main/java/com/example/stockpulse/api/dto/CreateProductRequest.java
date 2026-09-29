package com.example.stockpulse.api.dto;

import com.example.stockpulse.domain.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        @NotNull Category category,
        @NotNull @DecimalMin("0.01") BigDecimal currentPrice,
        @PositiveOrZero int stockLevel,
        @PositiveOrZero int reorderThreshold) {
}
