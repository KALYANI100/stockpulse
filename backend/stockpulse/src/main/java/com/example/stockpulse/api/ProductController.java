package com.example.stockpulse.api;

import com.example.stockpulse.api.dto.CreateProductRequest;
import com.example.stockpulse.api.dto.ProductMetricsRequest;
import com.example.stockpulse.api.dto.ProductResponse;
import com.example.stockpulse.api.dto.SimulateSaleRequest;
import com.example.stockpulse.api.dto.StockUpdateRequest;
import com.example.stockpulse.api.dto.SuggestionResponse;
import com.example.stockpulse.domain.Category;
import com.example.stockpulse.domain.ProductStatus;
import com.example.stockpulse.domain.SuggestionType;
import com.example.stockpulse.service.ProductService;
import com.example.stockpulse.service.RecommendationGenerationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    private final RecommendationGenerationService recommendationGenerationService;
    private final Executor recommendationExecutor;

    public ProductController(ProductService productService,
                             RecommendationGenerationService recommendationGenerationService,
                             @Qualifier("recommendationExecutor") Executor recommendationExecutor) {
        this.productService = productService;
        this.recommendationGenerationService = recommendationGenerationService;
        this.recommendationExecutor = recommendationExecutor;
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) Category category,
                                      @RequestParam(required = false) ProductStatus status) {
        return productService.list(category, status);
    }

    @GetMapping("/{productId}")
    public ProductResponse get(@PathVariable UUID productId) {
        return productService.get(productId);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse product = productService.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + product.id())).body(product);
    }

    @PatchMapping("/{productId}/stock")
    public ProductResponse updateStock(@PathVariable UUID productId,
                                       @Valid @RequestBody StockUpdateRequest request) {
        return productService.updateStock(productId, request.stockLevel());
    }

    @PatchMapping("/{productId}/metrics")
    public ProductResponse updateMetrics(@PathVariable UUID productId,
                                        @Valid @RequestBody ProductMetricsRequest request) {
        return productService.updateMetrics(productId, request.demandVelocity(), request.reorderThreshold());
    }

    @PostMapping("/{productId}/orders")
    public ProductResponse simulateSale(@PathVariable UUID productId,
                                        @Valid @RequestBody SimulateSaleRequest request) {
        return productService.simulateSale(productId, request.quantity());
    }

    @PostMapping("/{productId}/suggest-pricing")
    public ResponseEntity<Void> suggestPricing(@PathVariable UUID productId) {
        productService.requestAdvice(productId, SuggestionType.PRICING);
        return ResponseEntity.accepted().build();
    }

    @PostMapping(value = "/{productId}/suggest-pricing/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suggestPricingStream(@PathVariable UUID productId) {
        SseEmitter emitter = new SseEmitter(120_000L);
        emitter.onTimeout(emitter::complete);
        recommendationExecutor.execute(() -> {
            try {
                var suggestion = recommendationGenerationService.generatePricingStream(productId, token -> {
                    try {
                        emitter.send(SseEmitter.event().name("reasoning").data(Map.of("token", token)));
                    } catch (java.io.IOException exception) {
                        throw new IllegalStateException("SSE client disconnected", exception);
                    }
                });
                emitter.send(SseEmitter.event().name("suggestion")
                        .data(SuggestionResponse.from(suggestion)));
                emitter.complete();
            } catch (Exception exception) {
                String message = exception instanceof org.springframework.web.server.ResponseStatusException status
                        && status.getReason() != null
                        ? status.getReason()
                        : "Could not generate a pricing suggestion";
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("message", message)));
                } catch (java.io.IOException ignored) {
                    // Client disconnected before the error event could be sent.
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    @PostMapping("/{productId}/suggest-reorder")
    public ResponseEntity<Void> suggestReorder(@PathVariable UUID productId) {
        productService.requestAdvice(productId, SuggestionType.REORDER);
        return ResponseEntity.accepted().build();
    }
}
