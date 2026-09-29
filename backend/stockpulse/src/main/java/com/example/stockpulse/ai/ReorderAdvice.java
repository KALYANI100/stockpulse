package com.example.stockpulse.ai;

public record ReorderAdvice(int recommendedQuantity, int leadTimeDays,
                            double confidence, String reasoning) {
}
