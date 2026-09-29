package com.example.stockpulse.api.dto;

import com.example.stockpulse.domain.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

public record DecisionRequest(@NotNull SuggestionStatus decision) {
}
