package com.example.stockpulse.ai;

import com.example.stockpulse.domain.PriceDirection;

import java.math.BigDecimal;

public record PricingAdvice(BigDecimal recommendedPrice, PriceDirection direction,
                            double confidence, String reasoning) {
}
