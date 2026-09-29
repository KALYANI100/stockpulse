package com.example.stockpulse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_snapshots")
public class InventorySnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int stockLevel;

    @Column(nullable = false)
    private int demandVelocity;

    @Column(nullable = false)
    private Instant capturedAt;

    protected InventorySnapshot() {
    }

    public InventorySnapshot(UUID productId, int stockLevel, int demandVelocity) {
        this.productId = productId;
        this.stockLevel = stockLevel;
        this.demandVelocity = demandVelocity;
        this.capturedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public int getStockLevel() { return stockLevel; }
    public int getDemandVelocity() { return demandVelocity; }
    public Instant getCapturedAt() { return capturedAt; }
}
