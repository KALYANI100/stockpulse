package com.example.stockpulse.service;

import com.example.stockpulse.events.RecommendationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RecommendationEventListener {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationEventListener.class);
    private final RecommendationGenerationService generationService;

    public RecommendationEventListener(RecommendationGenerationService generationService) {
        this.generationService = generationService;
    }

    @Async("recommendationExecutor")
    @TransactionalEventListener
    public void onRecommendationRequested(RecommendationRequestedEvent event) {
        try {
            generationService.generate(event);
        } catch (RuntimeException exception) {
            logger.error("Recommendation generation failed for product {} and trigger {}",
                    event.productId(), event.triggerReason(), exception);
        }
    }
}
