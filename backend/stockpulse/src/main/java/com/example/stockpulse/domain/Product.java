package com.example.stockpulse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false)
    private int stockLevel;

    @Column(nullable = false)
    private int reorderThreshold;

    @Column(nullable = false)
    private int demandVelocity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductStatus status;

    @Version
    private long version;

    protected Product() {
    }

    public Product(String sku, String name, Category category, BigDecimal currentPrice,
                   int stockLevel, int reorderThreshold) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.currentPrice = currentPrice;
        this.stockLevel = stockLevel;
        this.reorderThreshold = reorderThreshold;
        this.demandVelocity = 0;
        this.status = stockLevel == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE;
    }

    public void updateStock(int newStockLevel) {
        if (newStockLevel < 0) {
            throw new IllegalArgumentException("Stock level cannot be negative");
        }
        boolean hasPendingSuggestions = status == ProductStatus.PRICE_REVIEW_PENDING;
        stockLevel = newStockLevel;
        refreshStatus(hasPendingSuggestions);
    }

    public void recordSale(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Sale quantity must be positive");
        }
        if (quantity > stockLevel) {
            throw new IllegalArgumentException("Sale quantity exceeds available stock");
        }
        boolean hasPendingSuggestions = status == ProductStatus.PRICE_REVIEW_PENDING;
        stockLevel -= quantity;
        demandVelocity += quantity;
        refreshStatus(hasPendingSuggestions);
    }

    public void updatePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.signum() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        currentPrice = newPrice;
    }

    public void markReviewPending() {
        if (stockLevel > 0) {
            status = ProductStatus.PRICE_REVIEW_PENDING;
        }
    }

    public void refreshStatus(boolean hasPendingSuggestions) {
        if (stockLevel == 0) {
            status = ProductStatus.OUT_OF_STOCK;
        } else if (hasPendingSuggestions) {
            status = ProductStatus.PRICE_REVIEW_PENDING;
        } else {
            status = ProductStatus.ACTIVE;
        }
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public int getStockLevel() { return stockLevel; }
    public int getReorderThreshold() { return reorderThreshold; }
    public int getDemandVelocity() { return demandVelocity; }
    public ProductStatus getStatus() { return status; }
}
