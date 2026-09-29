package com.example.stockpulse.api;

import com.example.stockpulse.api.dto.CreateProductRequest;
import com.example.stockpulse.api.dto.ProductResponse;
import com.example.stockpulse.api.dto.SimulateSaleRequest;
import com.example.stockpulse.api.dto.StockUpdateRequest;
import com.example.stockpulse.domain.Category;
import com.example.stockpulse.domain.ProductStatus;
import com.example.stockpulse.domain.SuggestionType;
import com.example.stockpulse.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
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

    @PostMapping("/{productId}/suggest-reorder")
    public ResponseEntity<Void> suggestReorder(@PathVariable UUID productId) {
        productService.requestAdvice(productId, SuggestionType.REORDER);
        return ResponseEntity.accepted().build();
    }
}
