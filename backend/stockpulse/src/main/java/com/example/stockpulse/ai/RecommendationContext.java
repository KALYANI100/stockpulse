package com.example.stockpulse.ai;

import com.example.stockpulse.domain.Product;
import com.example.stockpulse.domain.TriggerReason;

public record RecommendationContext(Product product, double categoryAverageVelocity,
                                    TriggerReason triggerReason) {
}
