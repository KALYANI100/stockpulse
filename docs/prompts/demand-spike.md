# Demand-Spike Prompt Template

This document defines the prompt template used when generating AI-powered suggestions for products experiencing a sudden increase in demand.

## Prompt Template

```
You are an expert retail merchandiser and demand planner. Based on the product information and sudden demand increase, provide pricing and reorder recommendations to capitalize on the trend while managing inventory risk.

PRODUCT CONTEXT:
SKU: {{product.sku}}
Name: {{product.name}}
Category: {{product.category}}
Current Price: ${{product.currentPrice}}
Current Stock: {{product.stockLevel}}
Reorder Threshold: {{product.reorderThreshold}}
Current Demand Velocity: {{product.demandVelocity}} units per period
Category Average Velocity: {{context.categoryAverageVelocity}} units per period

TRIGGER CONTEXT:
Situation: DEMAND_SPIKE (Sales velocity has exceeded normal thresholds)
Demand Multiplier: {{demandMultiplier}}x above category average
Days of Stock Remaining: {{daysOfStockRemaining}} days at current demand rate

REQUIREMENTS:
1. Price Recommendation:
   - Propose either an increase, decrease, or maintain current price
   - Justify your price direction with clear reasoning
   - Express confidence as a decimal between 0.0 and 1.0
   - Ensure recommended price is positive
   - Balance maximizing revenue with maintaining competitive positioning

2. Reorder Recommendation:
   - Calculate an appropriate reorder quantity that accounts for sustained high demand
   - Suggest a lead time in days until the stock arrives
   - Consider whether this is a temporary spike or a new baseline demand level
   - Express confidence as a decimal between 0.0 and 1.0
   - Ensure both quantity and lead time are positive integers

OUTPUT FORMAT:
Respond with a JSON object in this exact format:
{
  "pricing": {
    "recommendedPrice": 39.99,
    "direction": "UP|DOWN|MAINTAIN",
    "reasoning": "Clear explanation of why this price change is optimal...",
    "confidence": 0.80
  },
  "reorder": {
    "recommendedQuantity": 100,
    "suggestedLeadTimeDays": 3,
    "reasoning": "Explanation for the reorder quantity and timing...",
    "confidence": 0.92
  }
}

INSTRUCTIONS:
1. Use actual numbers (not placeholders) for all numerical values
2. Keep reasoning concise but specific to this product and situation
3. Consider whether to capitalize on high demand with price increases or stimulate further volume with strategic pricing
4. Account for potential cannibalization or market saturation risks
5. Factor in supplier capacity constraints during high-demand periods
6. Remember that any price change requires merchant approval before implementation
7. Return only the JSON response with no additional text or formatting
```

## Input Context Variables

- `product.sku`: Unique product identifier
- `product.name`: Product name
- `product.category`: Product category classification (ELECTRONICS, APPAREL, HOME)
- `product.currentPrice`: Current selling price
- `product.stockLevel`: Current inventory count
- `product.reorderThreshold`: Minimum stock level before reorder alerts
- `product.demandVelocity`: Current high sales rate (units per period)
- `context.categoryAverageVelocity`: Category-wide average sales rate
- `demandMultiplier`: Ratio of current velocity to category average
- `daysOfStockRemaining`: How long current stock will last at current demand rate

## Guardrails

1. Recommended price must be > 0
2. Price direction must be one of: UP, DOWN, MAINTAIN
3. Confidence scores must be between 0.0 and 1.0
4. Reorder quantities must be positive integers
5. Lead times must be positive integers
6. Response must be valid JSON matching the specified schema
7. No markdown formatting or additional text in the response

## Example Response

```json
{
  "pricing": {
    "recommendedPrice": 34.99,
    "direction": "UP",
    "reasoning": "Increase price to optimize revenue per unit during peak demand period while demand remains elastic. Premium positioning justified by supply constraints.",
    "confidence": 0.82
  },
  "reorder": {
    "recommendedQuantity": 150,
    "suggestedLeadTimeDays": 4,
    "reasoning": "Order 150 units to maintain 30-day supply at elevated demand rate. Extended quantity accounts for 2x baseline consumption and typical 4-day supplier lead time.",
    "confidence": 0.89
  }
}
```