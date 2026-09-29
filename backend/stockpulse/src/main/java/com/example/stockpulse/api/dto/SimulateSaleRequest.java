package com.example.stockpulse.api.dto;

import jakarta.validation.constraints.Positive;

public record SimulateSaleRequest(@Positive int quantity) {
}
