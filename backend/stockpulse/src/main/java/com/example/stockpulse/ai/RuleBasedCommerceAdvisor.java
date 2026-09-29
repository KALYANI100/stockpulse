package com.example.stockpulse.ai;

import com.example.stockpulse.domain.PriceDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class RuleBasedCommerceAdvisor {
    public AdviceBundle advise(RecommendationContext context) {
        var product = context.product();
        BigDecimal recommendedPrice = product.getCurrentPrice();
        String pricingReasoning = "Demand and stock are within normal ranges; hold the current price.";

        if (product.getStockLevel() < product.getReorderThreshold()) {
            recommendedPrice = recommendedPrice.multiply(new BigDecimal("1.10"))
                    .setScale(2, RoundingMode.HALF_UP);
            pricingReasoning = "Stock is below the reorder threshold; a modest increase protects remaining inventory.";
        } else if (product.getDemandVelocity() > 2 * context.categoryAverageVelocity()) {
            recommendedPrice = recommendedPrice.multiply(new BigDecimal("1.05"))
                    .setScale(2, RoundingMode.HALF_UP);
            pricingReasoning = "Demand is more than twice the category average; a modest increase responds to the spike.";
        }

        PriceDirection direction = recommendedPrice.compareTo(product.getCurrentPrice()) > 0
                ? PriceDirection.INCREASE
                : recommendedPrice.compareTo(product.getCurrentPrice()) < 0
                ? PriceDirection.DECREASE : PriceDirection.HOLD;
        int recommendedQuantity = Math.max(1,
                (product.getReorderThreshold() * 3) - product.getStockLevel());
        String reorderReasoning = "Replenish toward three times the reorder threshold.";

        return new AdviceBundle(
                new PricingAdvice(recommendedPrice, direction, 0.68, pricingReasoning),
                new ReorderAdvice(recommendedQuantity, 7, 0.65, reorderReasoning));
    }
}
