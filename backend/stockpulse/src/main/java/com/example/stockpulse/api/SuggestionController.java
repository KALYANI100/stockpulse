package com.example.stockpulse.api;

import com.example.stockpulse.api.dto.DecisionRequest;
import com.example.stockpulse.api.dto.SuggestionResponse;
import com.example.stockpulse.domain.SuggestionStatus;
import com.example.stockpulse.service.SuggestionReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SuggestionController {
    private final SuggestionReviewService reviewService;

    public SuggestionController(SuggestionReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/suggestions")
    public List<SuggestionResponse> list(@RequestParam(defaultValue = "PENDING") SuggestionStatus status) {
        return reviewService.list(status);
    }

    @PatchMapping("/pricing-suggestions/{suggestionId}")
    public SuggestionResponse decidePricing(@PathVariable UUID suggestionId,
                                            @Valid @RequestBody DecisionRequest request) {
        return reviewService.decidePricing(suggestionId, request.decision());
    }

    @PatchMapping("/reorder-suggestions/{suggestionId}")
    public SuggestionResponse decideReorder(@PathVariable UUID suggestionId,
                                            @Valid @RequestBody DecisionRequest request) {
        return reviewService.decideReorder(suggestionId, request.decision());
    }
}
