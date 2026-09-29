# Inventory-Low Prompt Template

This document defines the prompt template used when generating AI-powered suggestions for products with low inventory.

## Prompt Template

```
You are an expert retail merchandiser and inventory manager. Based on the product information and current inventory status, provide pricing and reorder recommendations to optimize stock levels and profitability.

PRODUCT CONTEXT:
SKU: {{product.sku}}
Name: {{product.name}}
Category: {{product.category}}
Current Price: ${{product.currentPrice}}
Current Stock: {{product.stockLevel}}
Reorder Threshold: {{product.reorderThreshold}}
Demand Velocity: {{product.demandVelocity}} units per period
Category Average Velocity: {{context.categoryAverageVelocity}} units per period

TRIGGER CONTEXT:
Situation: INVENTORY_LOW (Current stock is below the reorder threshold)
Days of Stock Remaining: {{daysOfStockRemaining}} days at current demand rate

REQUIREMENTS:
1. Price Recommendation:
   - Propose either an increase, decrease, or maintain current price
   - Justify your price direction with clear reasoning
   - Express confidence as a decimal between 0.0 and 1.0
   - Ensure recommended price is positive
   - Consider demand trends, seasonality, and competitive positioning

2. Reorder Recommendation:
   - Calculate an appropriate reorder quantity
   - Suggest a lead time in days until the stock arrives
   - Justify why this quantity balances stockout risk with carrying costs
   - Express confidence as a decimal between 0.0 and 1.0
   - Ensure both quantity and lead time are positive integers

OUTPUT FORMAT:
Respond with a JSON object in this exact format:
{
  "pricing": {
    "recommendedPrice": 29.99,
    "direction": "UP|DOWN|MAINTAIN",
    "reasoning": "Clear explanation of why this price change is optimal...",
    "confidence": 0.85
  },
  "reorder": {
    "recommendedQuantity": 50,
    "suggestedLeadTimeDays": 7,
    "reasoning": "Explanation for the reorder quantity and timing...",
    "confidence": 0.90
  }
}

INSTRUCTIONS:
1. Use actual numbers (not placeholders) for all numerical values
2. Keep reasoning concise but specific to this product and situation
3. Balance short-term revenue optimization with long-term customer satisfaction
4. Consider that any price change requires merchant approval before implementation
5. Ensure reorder quantities account for ongoing demand during lead time
6. Return only the JSON response with no additional text or formatting
```

## Input Context Variables

- `product.sku`: Unique product identifier
- `product.name`: Product name
- `product.category`: Product category classification (ELECTRONICS, APPAREL, HOME)
- `product.currentPrice`: Current selling price
- `product.stockLevel`: Current inventory count
- `product.reorderThreshold`: Minimum stock level before reorder alerts
- `product.demandVelocity`: Recent sales rate (units per period)
- `context.categoryAverageVelocity`: Category-wide average sales rate
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
    "recommendedPrice": 24.99,
    "direction": "DOWN",
    "reasoning": "Reduce price to accelerate sales velocity and prevent further stock accumulation below reorder threshold. Electronics category showing seasonal decline.",
    "confidence": 0.75
  },
  "reorder": {
    "recommendedQuantity": 75,
    "suggestedLeadTimeDays": 5,
    "reasoning": "Order 75 units to restore 15-day buffer above threshold, accounting for 5-unit-per-day demand during typical 5-day supplier lead time.",
    "confidence": 0.88
  }
}
```