package com.example.stockpulse.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class CommerceAdvisorService {
    private static final Logger logger = LoggerFactory.getLogger(CommerceAdvisorService.class);

    private final AiCommerceAdvisor aiAdvisor;
    private final RuleBasedCommerceAdvisor ruleAdvisor;
    private final LlmGateway gateway;
    private final StrategyState strategyState;

    public CommerceAdvisorService(AiCommerceAdvisor aiAdvisor,
                                  RuleBasedCommerceAdvisor ruleAdvisor,
                                  LlmGateway gateway,
                                  StrategyState strategyState) {
        this.aiAdvisor = aiAdvisor;
        this.ruleAdvisor = ruleAdvisor;
        this.gateway = gateway;
        this.strategyState = strategyState;
    }

    public AdviceBundle advise(RecommendationContext context) {
        AdvisorMode mode = strategyState.getMode();
        if (mode != AdvisorMode.RULES && gateway.isConfigured()) {
            try {
                return aiAdvisor.advise(context);
            } catch (RuntimeException exception) {
                logger.warn("AI advisor failed; using rule-based recommendations: {}", exception.getMessage());
            }
        }
        return ruleAdvisor.advise(context);
    }

    public com.example.stockpulse.ai.PricingAdvice advisePricingStreaming(
            RecommendationContext context, Consumer<String> onReasoning) {
        if (strategyState.getMode() != AdvisorMode.RULES && gateway.isConfigured()) {
            try {
                return aiAdvisor.advisePricingStreaming(context, onReasoning);
            } catch (RuntimeException exception) {
                logger.warn("AI pricing stream failed; using rule-based fallback: {}", exception.getMessage());
                onReasoning.accept("\n\nAI advice was unavailable, so rule-based pricing was used. ");
            }
        } else {
            onReasoning.accept("Rule-based pricing guidance. ");
        }
        var fallback = ruleAdvisor.advise(context).pricing();
        onReasoning.accept(fallback.reasoning());
        return fallback;
    }
}
