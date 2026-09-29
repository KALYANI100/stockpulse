package com.example.stockpulse.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class StrategyState {
    private final AtomicReference<AdvisorMode> mode;

    public StrategyState(@Value("${app.recommendations.strategy:AUTO}") String initialMode) {
        this.mode = new AtomicReference<>(AdvisorMode.valueOf(initialMode.toUpperCase()));
    }

    public AdvisorMode getMode() {
        return mode.get();
    }

    public AdvisorMode setMode(AdvisorMode newMode) {
        mode.set(newMode);
        return newMode;
    }
}
