package com.example.stockpulse.service;

import com.example.stockpulse.api.dto.CreateProductRequest;
import com.example.stockpulse.api.dto.ProductResponse;
import com.example.stockpulse.domain.Category;
import com.example.stockpulse.domain.InventorySnapshot;
import com.example.stockpulse.domain.Product;
import com.example.stockpulse.domain.ProductStatus;
import com.example.stockpulse.domain.SuggestionType;
import com.example.stockpulse.domain.SuggestionStatus;
import com.example.stockpulse.domain.TriggerReason;
import com.example.stockpulse.events.RecommendationRequestedEvent;
import com.example.stockpulse.repository.InventorySnapshotRepository;
import com.example.stockpulse.repository.PricingSuggestionRepository;
import com.example.stockpulse.repository.ProductRepository;
import com.example.stockpulse.repository.ReorderSuggestionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingRepository;
    private final ReorderSuggestionRepository reorderRepository;
    private final InventorySnapshotRepository snapshotRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final double demandSpikeMultiplier;

    public ProductService(ProductRepository productRepository,
                          PricingSuggestionRepository pricingRepository,
                          ReorderSuggestionRepository reorderRepository,
                          InventorySnapshotRepository snapshotRepository,
                          ApplicationEventPublisher eventPublisher,
                          @Value("${app.recommendations.demand-spike-multiplier:3.0}") double demandSpikeMultiplier) {
        this.productRepository = productRepository;
        this.pricingRepository = pricingRepository;
        this.reorderRepository = reorderRepository;
        this.snapshotRepository = snapshotRepository;
        this.eventPublisher = eventPublisher;
        this.demandSpikeMultiplier = demandSpikeMultiplier;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (productRepository.findBySku(request.sku()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists");
        }
        Product product = productRepository.save(new Product(request.sku().trim(), request.name().trim(),
                request.category(), request.currentPrice(), request.stockLevel(), request.reorderThreshold()));
        saveSnapshot(product);
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(Category category, ProductStatus status) {
        return productRepository.findAllByOrderByNameAsc().stream()
                .filter(product -> category == null || product.getCategory() == category)
                .filter(product -> status == null || product.getStatus() == status)
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID productId) {
        return ProductResponse.from(requireProduct(productId));
    }

    @Transactional
    public ProductResponse updateStock(UUID productId, int stockLevel) {
        Product product = requireProduct(productId);
        product.updateStock(stockLevel);
        refreshStatus(product);
        saveSnapshot(product);
        if (product.getStockLevel() < product.getReorderThreshold()) {
            publish(product, TriggerReason.INVENTORY_LOW, null);
        }
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateMetrics(UUID productId, int demandVelocity, int reorderThreshold) {
        Product product = requireProduct(productId);
        product.updateDemandVelocity(demandVelocity);
        product.updateReorderThreshold(reorderThreshold);
        refreshStatus(product);
        saveSnapshot(product);

        if (product.getStockLevel() < product.getReorderThreshold()) {
            publish(product, TriggerReason.INVENTORY_LOW, null);
        }
        if (isDemandSpike(product)) {
            publish(product, TriggerReason.DEMAND_SPIKE, null);
        }
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse simulateSale(UUID productId, int quantity) {
        Product product = requireProduct(productId);
        product.recordSale(quantity);
        refreshStatus(product);
        saveSnapshot(product);

        if (product.getStockLevel() < product.getReorderThreshold()) {
            publish(product, TriggerReason.INVENTORY_LOW, null);
        }
        if (isDemandSpike(product)) {
            publish(product, TriggerReason.DEMAND_SPIKE, null);
        }
        return ProductResponse.from(product);
    }

    @Transactional
    public void requestAdvice(UUID productId, SuggestionType type) {
        requireProduct(productId);
        publish(productId, TriggerReason.MANUAL, type);
    }

    private boolean isDemandSpike(Product product) {
        List<Product> peers = productRepository.findByCategory(product.getCategory()).stream()
                .filter(peer -> !peer.getId().equals(product.getId()))
                .toList();
        if (peers.isEmpty()) {
            return false;
        }
        double averageVelocity = peers.stream().mapToInt(Product::getDemandVelocity).average().orElse(0);
        return averageVelocity > 0
                && product.getDemandVelocity() > averageVelocity * demandSpikeMultiplier;
    }

    private Product requireProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void refreshStatus(Product product) {
        boolean hasPending = pricingRepository.existsByProductIdAndStatus(product.getId(), SuggestionStatus.PENDING)
                || reorderRepository.existsByProductIdAndStatus(product.getId(), SuggestionStatus.PENDING);
        product.refreshStatus(hasPending);
    }

    private void saveSnapshot(Product product) {
        snapshotRepository.save(new InventorySnapshot(product.getId(), product.getStockLevel(),
                product.getDemandVelocity()));
    }

    private void publish(Product product, TriggerReason reason, SuggestionType type) {
        publish(product.getId(), reason, type);
    }

    private void publish(UUID productId, TriggerReason reason, SuggestionType type) {
        eventPublisher.publishEvent(new RecommendationRequestedEvent(productId, reason, type));
    }
}
