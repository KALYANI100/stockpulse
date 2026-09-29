package com.example.stockpulse.api.dto;

import com.example.stockpulse.ai.AdvisorMode;
import jakarta.validation.constraints.NotNull;

public record StrategyRequest(@NotNull AdvisorMode mode) {
}
