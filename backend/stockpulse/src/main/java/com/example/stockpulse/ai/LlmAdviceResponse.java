package com.example.stockpulse.ai;

import com.example.stockpulse.domain.PriceDirection;

import java.math.BigDecimal;

public record LlmAdviceResponse(BigDecimal recommendedPrice, PriceDirection direction,
                                double pricingConfidence, String pricingReasoning,
                                int recommendedQuantity, int leadTimeDays,
                                double reorderConfidence, String reorderReasoning) {
}
