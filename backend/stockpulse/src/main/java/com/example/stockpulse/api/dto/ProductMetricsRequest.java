package com.example.stockpulse.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductMetricsRequest(
        @NotNull @PositiveOrZero Integer demandVelocity,
        @NotNull @PositiveOrZero Integer reorderThreshold) {
}
