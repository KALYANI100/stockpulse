package com.example.stockpulse.api.dto;

import com.example.stockpulse.domain.PriceDirection;
import com.example.stockpulse.domain.PricingSuggestion;
import com.example.stockpulse.domain.ReorderSuggestion;
import com.example.stockpulse.domain.SuggestionStatus;
import com.example.stockpulse.domain.SuggestionType;
import com.example.stockpulse.domain.TriggerReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SuggestionResponse(UUID id, UUID productId, String productName,
                                 SuggestionType type, SuggestionStatus status,
                                 TriggerReason triggerReason, double confidence,
                                 String reasoning, BigDecimal currentPrice,
                                 BigDecimal recommendedPrice, PriceDirection direction,
                                 Integer currentStock, Integer recommendedQuantity,
                                 Integer suggestedLeadTimeDays, Instant createdAt) {
    public static SuggestionResponse from(PricingSuggestion suggestion) {
        return new SuggestionResponse(suggestion.getId(), suggestion.getProductId(),
                suggestion.getProductName(), SuggestionType.PRICING, suggestion.getStatus(),
                suggestion.getTriggerReason(), suggestion.getConfidence(), suggestion.getReasoning(),
                suggestion.getCurrentPrice(), suggestion.getRecommendedPrice(), suggestion.getDirection(),
                null, null, null, suggestion.getCreatedAt());
    }

    public static SuggestionResponse from(ReorderSuggestion suggestion) {
        return new SuggestionResponse(suggestion.getId(), suggestion.getProductId(),
                suggestion.getProductName(), SuggestionType.REORDER, suggestion.getStatus(),
                suggestion.getTriggerReason(), suggestion.getConfidence(), suggestion.getReasoning(),
                null, null, null, suggestion.getCurrentStock(), suggestion.getRecommendedQuantity(),
                suggestion.getSuggestedLeadTimeDays(), suggestion.getCreatedAt());
    }
}
