package com.example.stockpulse.api;

import com.example.stockpulse.ai.LlmGateway;
import com.example.stockpulse.ai.StrategyState;
import com.example.stockpulse.api.dto.StrategyRequest;
import com.example.stockpulse.api.dto.StrategyResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/strategy")
public class StrategyController {
    private final StrategyState strategyState;
    private final LlmGateway gateway;

    public StrategyController(StrategyState strategyState, LlmGateway gateway) {
        this.strategyState = strategyState;
        this.gateway = gateway;
    }

    @GetMapping
    public StrategyResponse get() {
        return new StrategyResponse(strategyState.getMode(), gateway.isConfigured());
    }

    @PatchMapping
    public StrategyResponse update(@Valid @RequestBody StrategyRequest request) {
        return new StrategyResponse(strategyState.setMode(request.mode()), gateway.isConfigured());
    }
}
