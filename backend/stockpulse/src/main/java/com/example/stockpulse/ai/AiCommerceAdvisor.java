package com.example.stockpulse.ai;

import com.example.stockpulse.domain.PriceDirection;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AiCommerceAdvisor {
    private final LlmGateway gateway;
    private final ObjectMapper objectMapper;

    public AiCommerceAdvisor(LlmGateway gateway, ObjectMapper objectMapper) {
        this.gateway = gateway;
        this.objectMapper = objectMapper;
    }

    public AdviceBundle advise(RecommendationContext context) {
        try {
            String json = gateway.complete(buildPrompt(context));
            String normalizedJson = json.trim()
                    .replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "");
            LlmAdviceResponse response = objectMapper.readValue(normalizedJson, LlmAdviceResponse.class);
            validate(response, context);
            return new AdviceBundle(
                    new PricingAdvice(response.recommendedPrice().setScale(2, RoundingMode.HALF_UP),
                            response.direction(), response.pricingConfidence(), response.pricingReasoning().trim()),
                    new ReorderAdvice(response.recommendedQuantity(), response.leadTimeDays(),
                            response.reorderConfidence(), response.reorderReasoning().trim()));
        } catch (Exception exception) {
            throw new IllegalStateException("AI recommendation was unavailable or invalid", exception);
        }
    }

    private String buildPrompt(RecommendationContext context) {
        var product = context.product();
        String triggerInstructions = switch (context.triggerReason()) {
            case INVENTORY_LOW -> "Stock is below the reorder threshold. Consider whether to protect scarce inventory or clear it, and explain that tradeoff.";
            case DEMAND_SPIKE -> "Demand has spiked relative to category peers. Consider a modest price response and replenishment for elevated demand.";
            case MANUAL, INITIAL -> "This is a manual merchandising review. Recommend a balanced price and replenishment decision.";
        };
        return "You are a cautious commerce advisor. Return only one JSON object with fields "
                + "recommendedPrice, direction (INCREASE, DECREASE, HOLD), pricingConfidence, pricingReasoning, "
                + "recommendedQuantity, leadTimeDays, reorderConfidence, reorderReasoning. "
                + "Use a positive price no more than 4x or less than 0.25x the current price; quantity must be a positive integer. "
                + "Keep both confidence values between 0 and 1. Do not claim any action was applied.\n"
                + "Trigger: " + context.triggerReason() + ". " + triggerInstructions + "\n"
                + "Product: " + product.getName() + "; category: " + product.getCategory()
                + "; current price: " + product.getCurrentPrice()
                + "; stock: " + product.getStockLevel()
                + "; reorder threshold: " + product.getReorderThreshold()
                + "; demand velocity: " + product.getDemandVelocity()
                + "; peer category average velocity: " + context.categoryAverageVelocity() + ".";
    }

    private void validate(LlmAdviceResponse response, RecommendationContext context) {
        if (response == null || response.recommendedPrice() == null
                || response.recommendedPrice().signum() <= 0
                || response.recommendedQuantity() <= 0 || response.recommendedQuantity() > 100_000
                || response.leadTimeDays() < 0 || response.leadTimeDays() > 365
                || !validConfidence(response.pricingConfidence())
                || !validConfidence(response.reorderConfidence())
                || response.direction() == null
                || response.pricingReasoning() == null || response.pricingReasoning().isBlank()
                || response.reorderReasoning() == null || response.reorderReasoning().isBlank()) {
            throw new IllegalArgumentException("LLM returned invalid recommendation fields");
        }
        BigDecimal currentPrice = context.product().getCurrentPrice();
        if (response.recommendedPrice().compareTo(currentPrice.multiply(new BigDecimal("0.25"))) < 0
                || response.recommendedPrice().compareTo(currentPrice.multiply(new BigDecimal("4"))) > 0) {
            throw new IllegalArgumentException("LLM recommended price is outside configured bounds");
        }
        PriceDirection expectedDirection = response.recommendedPrice().compareTo(currentPrice) > 0
                ? PriceDirection.INCREASE
                : response.recommendedPrice().compareTo(currentPrice) < 0
                ? PriceDirection.DECREASE : PriceDirection.HOLD;
        if (expectedDirection != response.direction()) {
            throw new IllegalArgumentException("LLM price direction does not match its recommended price");
        }
    }

    private boolean validConfidence(double confidence) {
        return Double.isFinite(confidence) && confidence >= 0 && confidence <= 1;
    }
}
