package com.example.stockpulse.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
}
