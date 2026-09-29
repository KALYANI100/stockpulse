package com.example.stockpulse.ai;

import com.example.stockpulse.domain.Category;
import com.example.stockpulse.domain.PriceDirection;
import com.example.stockpulse.domain.Product;
import com.example.stockpulse.domain.TriggerReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleBasedCommerceAdvisorTests {
    private final RuleBasedCommerceAdvisor advisor = new RuleBasedCommerceAdvisor();

    @Test
    void lowStockRecommendsPriceProtectionAndReplenishment() {
        Product product = new Product("SKU-1", "Low stock item", Category.HOME,
                new BigDecimal("20.00"), 4, 5);

        AdviceBundle advice = advisor.advise(new RecommendationContext(product, 2,
                TriggerReason.INVENTORY_LOW));

        assertEquals(new BigDecimal("22.00"), advice.pricing().recommendedPrice());
        assertEquals(PriceDirection.INCREASE, advice.pricing().direction());
        assertEquals(11, advice.reorder().recommendedQuantity());
    }

    @Test
    void normalConditionsHoldCurrentPrice() {
        Product product = new Product("SKU-2", "Steady item", Category.HOME,
                new BigDecimal("20.00"), 20, 5);

        AdviceBundle advice = advisor.advise(new RecommendationContext(product, 4,
                TriggerReason.MANUAL));

        assertEquals(new BigDecimal("20.00"), advice.pricing().recommendedPrice());
        assertEquals(PriceDirection.HOLD, advice.pricing().direction());
    }
}
