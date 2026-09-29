package com.example.stockpulse.api.dto;

import com.example.stockpulse.domain.Product;
import com.example.stockpulse.domain.ProductStatus;
import com.example.stockpulse.domain.Category;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(UUID id, String sku, String name, Category category,
                              BigDecimal currentPrice, int stockLevel, int reorderThreshold,
                              int demandVelocity, ProductStatus status) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(),
                product.getCategory(), product.getCurrentPrice(), product.getStockLevel(),
                product.getReorderThreshold(), product.getDemandVelocity(), product.getStatus());
    }
}
