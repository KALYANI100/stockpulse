package com.example.stockpulse.events;

import com.example.stockpulse.domain.SuggestionType;
import com.example.stockpulse.domain.TriggerReason;

import java.util.UUID;

public record RecommendationRequestedEvent(UUID productId, TriggerReason triggerReason,
                                           SuggestionType requestedType) {
}
