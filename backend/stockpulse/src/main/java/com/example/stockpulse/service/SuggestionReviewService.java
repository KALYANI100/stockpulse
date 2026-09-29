package com.example.stockpulse.service;

import com.example.stockpulse.api.dto.SuggestionResponse;
import com.example.stockpulse.domain.InventorySnapshot;
import com.example.stockpulse.domain.Product;
import com.example.stockpulse.domain.SuggestionStatus;
import com.example.stockpulse.repository.InventorySnapshotRepository;
import com.example.stockpulse.repository.PricingSuggestionRepository;
import com.example.stockpulse.repository.ProductRepository;
import com.example.stockpulse.repository.ReorderSuggestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class SuggestionReviewService {
    private final PricingSuggestionRepository pricingRepository;
    private final ReorderSuggestionRepository reorderRepository;
    private final ProductRepository productRepository;
    private final InventorySnapshotRepository snapshotRepository;

    public SuggestionReviewService(PricingSuggestionRepository pricingRepository,
                                   ReorderSuggestionRepository reorderRepository,
                                   ProductRepository productRepository,
                                   InventorySnapshotRepository snapshotRepository) {
        this.pricingRepository = pricingRepository;
        this.reorderRepository = reorderRepository;
        this.productRepository = productRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponse> list(SuggestionStatus status) {
        return Stream.concat(
                        pricingRepository.findAllByStatusOrderByCreatedAtDesc(status).stream()
                                .map(SuggestionResponse::from),
                        reorderRepository.findAllByStatusOrderByCreatedAtDesc(status).stream()
                                .map(SuggestionResponse::from))
                .sorted(Comparator.comparing(SuggestionResponse::createdAt).reversed())
                .toList();
    }

    @Transactional
    public SuggestionResponse decidePricing(UUID suggestionId, SuggestionStatus decision) {
        requireFinalDecision(decision);
        var suggestion = pricingRepository.findById(suggestionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pricing suggestion not found"));
        Product product = requireProduct(suggestion.getProductId());
        suggestion.decide(decision);
        if (decision == SuggestionStatus.ACCEPTED) {
            product.updatePrice(suggestion.getRecommendedPrice());
        }
        pricingRepository.saveAndFlush(suggestion);
        refreshProductStatus(product);
        return SuggestionResponse.from(suggestion);
    }

    @Transactional
    public SuggestionResponse decideReorder(UUID suggestionId, SuggestionStatus decision) {
        requireFinalDecision(decision);
        var suggestion = reorderRepository.findById(suggestionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reorder suggestion not found"));
        Product product = requireProduct(suggestion.getProductId());
        suggestion.decide(decision);
        if (decision == SuggestionStatus.ACCEPTED) {
            try {
                product.updateStock(Math.addExact(product.getStockLevel(), suggestion.getRecommendedQuantity()));
            } catch (ArithmeticException exception) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Accepted quantity exceeds stock capacity");
            }
            snapshotRepository.save(new InventorySnapshot(product.getId(), product.getStockLevel(),
                    product.getDemandVelocity()));
        }
        reorderRepository.saveAndFlush(suggestion);
        refreshProductStatus(product);
        return SuggestionResponse.from(suggestion);
    }

    private void refreshProductStatus(Product product) {
        boolean hasPending = pricingRepository.existsByProductIdAndStatus(product.getId(), SuggestionStatus.PENDING)
                || reorderRepository.existsByProductIdAndStatus(product.getId(), SuggestionStatus.PENDING);
        product.refreshStatus(hasPending);
    }

    private Product requireProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void requireFinalDecision(SuggestionStatus decision) {
        if (decision == null || decision == SuggestionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be ACCEPTED or REJECTED");
        }
    }
}
