package com.example.stockpulse.api.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record StockUpdateRequest(@PositiveOrZero int stockLevel) {
}
