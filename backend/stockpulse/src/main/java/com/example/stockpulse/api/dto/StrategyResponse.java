package com.example.stockpulse.api.dto;

import com.example.stockpulse.ai.AdvisorMode;

public record StrategyResponse(AdvisorMode mode, boolean llmConfigured) {
}
