package com.example.stockpulse.service;

import com.example.stockpulse.ai.CommerceAdvisorService;
import com.example.stockpulse.ai.RecommendationContext;
import com.example.stockpulse.domain.InventorySnapshot;
import com.example.stockpulse.domain.PricingSuggestion;
import com.example.stockpulse.domain.Product;
import com.example.stockpulse.domain.ReorderSuggestion;
import com.example.stockpulse.domain.SuggestionStatus;
import com.example.stockpulse.domain.SuggestionType;
import com.example.stockpulse.events.RecommendationRequestedEvent;
import com.example.stockpulse.repository.InventorySnapshotRepository;
import com.example.stockpulse.repository.PricingSuggestionRepository;
import com.example.stockpulse.repository.ProductRepository;
import com.example.stockpulse.repository.ReorderSuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class RecommendationGenerationService {
    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingRepository;
    private final ReorderSuggestionRepository reorderRepository;
    private final InventorySnapshotRepository snapshotRepository;
    private final CommerceAdvisorService advisor;

    public RecommendationGenerationService(ProductRepository productRepository,
                                           PricingSuggestionRepository pricingRepository,
                                           ReorderSuggestionRepository reorderRepository,
                                           InventorySnapshotRepository snapshotRepository,
                                           CommerceAdvisorService advisor) {
        this.productRepository = productRepository;
        this.pricingRepository = pricingRepository;
        this.reorderRepository = reorderRepository;
        this.snapshotRepository = snapshotRepository;
        this.advisor = advisor;
    }

    @Transactional
    public void generate(RecommendationRequestedEvent event) {
        Product product = productRepository.findById(event.productId()).orElse(null);
        if (product == null) {
            return;
        }

        boolean wantsPricing = event.requestedType() == null || event.requestedType() == SuggestionType.PRICING;
        boolean wantsReorder = event.requestedType() == null || event.requestedType() == SuggestionType.REORDER;
        boolean createPricing = wantsPricing && !pricingRepository.existsByProductIdAndTriggerReasonAndStatus(
                product.getId(), event.triggerReason(), SuggestionStatus.PENDING);
        boolean createReorder = wantsReorder && !reorderRepository.existsByProductIdAndTriggerReasonAndStatus(
                product.getId(), event.triggerReason(), SuggestionStatus.PENDING);
        if (!createPricing && !createReorder) {
            return;
        }

        double categoryAverage = productRepository.findByCategory(product.getCategory()).stream()
                .filter(peer -> !peer.getId().equals(product.getId()))
                .mapToInt(Product::getDemandVelocity)
                .average()
                .orElse(0);
        var advice = advisor.advise(new RecommendationContext(product, categoryAverage, event.triggerReason()));

        if (createPricing) {
            var pricing = advice.pricing();
            pricingRepository.save(new PricingSuggestion(product.getId(), product.getName(),
                    product.getCurrentPrice(), pricing.recommendedPrice(), pricing.direction(),
                    pricing.confidence(), pricing.reasoning(), event.triggerReason()));
        }
        if (createReorder) {
            var reorder = advice.reorder();
            reorderRepository.save(new ReorderSuggestion(product.getId(), product.getName(),
                    product.getStockLevel(), reorder.recommendedQuantity(), reorder.leadTimeDays(),
                    reorder.confidence(), reorder.reasoning(), event.triggerReason()));
        }
        product.markReviewPending();
        snapshotRepository.save(new InventorySnapshot(product.getId(), product.getStockLevel(),
                product.getDemandVelocity()));
    }

        @Transactional
        public PricingSuggestion generatePricingStream(UUID productId, Consumer<String> onReasoning) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (pricingRepository.existsByProductIdAndTriggerReasonAndStatus(
            product.getId(), com.example.stockpulse.domain.TriggerReason.MANUAL, SuggestionStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A pending manual pricing suggestion already exists for this product");
        }

        double categoryAverage = productRepository.findByCategory(product.getCategory()).stream()
            .filter(peer -> !peer.getId().equals(product.getId()))
            .mapToInt(Product::getDemandVelocity)
            .average()
            .orElse(0);
        var context = new RecommendationContext(product, categoryAverage,
            com.example.stockpulse.domain.TriggerReason.MANUAL);
        var pricing = advisor.advisePricingStreaming(context, onReasoning);
        PricingSuggestion suggestion = pricingRepository.save(new PricingSuggestion(
            product.getId(), product.getName(), product.getCurrentPrice(), pricing.recommendedPrice(),
            pricing.direction(), pricing.confidence(), pricing.reasoning(),
            com.example.stockpulse.domain.TriggerReason.MANUAL));
        product.markReviewPending();
        snapshotRepository.save(new InventorySnapshot(product.getId(), product.getStockLevel(),
            product.getDemandVelocity()));
        return suggestion;
        }
}
