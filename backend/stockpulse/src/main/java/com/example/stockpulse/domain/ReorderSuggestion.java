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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reorder_suggestions")
public class ReorderSuggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int currentStock;

    @Column(nullable = false)
    private int recommendedQuantity;

    @Column(nullable = false)
    private int suggestedLeadTimeDays;

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

    protected ReorderSuggestion() {
    }

    public ReorderSuggestion(UUID productId, String productName, int currentStock,
                             int recommendedQuantity, int suggestedLeadTimeDays,
                             double confidence, String reasoning, TriggerReason triggerReason) {
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.recommendedQuantity = recommendedQuantity;
        this.suggestedLeadTimeDays = suggestedLeadTimeDays;
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
    public int getCurrentStock() { return currentStock; }
    public int getRecommendedQuantity() { return recommendedQuantity; }
    public int getSuggestedLeadTimeDays() { return suggestedLeadTimeDays; }
    public double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public SuggestionStatus getStatus() { return status; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public Instant getCreatedAt() { return createdAt; }
}
