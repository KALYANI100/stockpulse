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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pricing_suggestions")
public class PricingSuggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal recommendedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PriceDirection direction;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false, length = 1200)
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SuggestionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TriggerReason triggerReason;

    @Column(nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected PricingSuggestion() {
    }

    public PricingSuggestion(UUID productId, String productName, BigDecimal currentPrice,
                             BigDecimal recommendedPrice, PriceDirection direction,
                             double confidence, String reasoning, TriggerReason triggerReason) {
        this.productId = productId;
        this.productName = productName;
        this.currentPrice = currentPrice;
        this.recommendedPrice = recommendedPrice;
        this.direction = direction;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.triggerReason = triggerReason;
        this.status = SuggestionStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void decide(SuggestionStatus decision) {
        if (status != SuggestionStatus.PENDING || decision == SuggestionStatus.PENDING) {
            throw new IllegalStateException("Only pending suggestions can be accepted or rejected");
        }
        status = decision;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getRecommendedPrice() { return recommendedPrice; }
    public PriceDirection getDirection() { return direction; }
    public double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public SuggestionStatus getStatus() { return status; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public Instant getCreatedAt() { return createdAt; }
}
