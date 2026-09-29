package com.example.stockpulse.ai;

import com.example.stockpulse.domain.PriceDirection;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;

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

    public PricingAdvice advisePricingStreaming(RecommendationContext context, Consumer<String> onReasoning) {
        String marker = "\n---STOCKPULSE_JSON---\n";
        StringBuilder streamedResponse = new StringBuilder();
        int[] emittedLength = {0};

        gateway.streamCompletion(buildStreamingPrompt(context), token -> {
            streamedResponse.append(token);
            int markerIndex = streamedResponse.indexOf(marker);
            if (markerIndex >= 0) {
                if (markerIndex > emittedLength[0]) {
                    onReasoning.accept(streamedResponse.substring(emittedLength[0], markerIndex));
                }
                emittedLength[0] = markerIndex;
                return;
            }

            int safeLength = Math.max(0, streamedResponse.length() - marker.length() + 1);
            if (safeLength > emittedLength[0]) {
                onReasoning.accept(streamedResponse.substring(emittedLength[0], safeLength));
                emittedLength[0] = safeLength;
            }
        });

        int markerIndex = streamedResponse.indexOf(marker);
        if (markerIndex < 0) {
            throw new IllegalStateException("LLM stream did not include the recommendation payload");
        }
        String json = streamedResponse.substring(markerIndex + marker.length()).trim();
        try {
            LlmAdviceResponse response = objectMapper.readValue(json, LlmAdviceResponse.class);
            validate(response, context);
            return new PricingAdvice(response.recommendedPrice().setScale(2, RoundingMode.HALF_UP),
                    response.direction(), response.pricingConfidence(), response.pricingReasoning().trim());
        } catch (Exception exception) {
            throw new IllegalStateException("AI pricing recommendation was invalid", exception);
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

    private String buildStreamingPrompt(RecommendationContext context) {
        var product = context.product();
        String triggerInstructions = switch (context.triggerReason()) {
            case INVENTORY_LOW -> "Stock is below its reorder threshold. Explain whether the price should protect scarce inventory or help clear it.";
            case DEMAND_SPIKE -> "Demand has spiked relative to peers. Explain a measured pricing response to elevated demand.";
            case MANUAL, INITIAL -> "This is a manual merchandising review. Explain the pricing recommendation briefly.";
        };
        return "You are a cautious commerce advisor. Stream a concise plain-language pricing rationale first. "
                + "Then write this exact delimiter on its own line: \n---STOCKPULSE_JSON---\n "
                + "After the delimiter, return exactly one valid JSON object with fields recommendedPrice, "
                + "direction (INCREASE, DECREASE, HOLD), pricingConfidence, pricingReasoning, "
                + "recommendedQuantity, leadTimeDays, reorderConfidence, and reorderReasoning. "
                + "Use a positive price between 0.25x and 4x the current price, a positive integer quantity, "
                + "and confidence values between 0 and 1. Do not use Markdown fences or claim an action was applied.\n"
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
