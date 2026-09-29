# StockPulse API

Base URL: `http://localhost:8080/api`. The local development API has no authentication. JSON request and response bodies use `application/json` except the pricing stream, which uses `text/event-stream`.

## Enums

- `Category`: `ELECTRONICS`, `APPAREL`, `HOME`
- `ProductStatus`: `ACTIVE`, `PRICE_REVIEW_PENDING`, `OUT_OF_STOCK`
- `SuggestionStatus`: `PENDING`, `ACCEPTED`, `REJECTED`
- `SuggestionType`: `PRICING`, `REORDER`
- `TriggerReason`: `INITIAL`, `INVENTORY_LOW`, `DEMAND_SPIKE`, `MANUAL`
- `PriceDirection`: `INCREASE`, `DECREASE`, `HOLD`
- Advisor mode: `AUTO`, `AI`, `RULES`

## Products

| Method and path | Behavior |
| --- | --- |
| `GET /products?category=&status=` | List products; filters are optional. |
| `GET /products/{productId}` | Get one product. |
| `POST /products` | Create a product. Initial demand velocity is zero. |
| `PATCH /products/{productId}/stock` | Set stock level; may queue low-stock advice asynchronously. |
| `PATCH /products/{productId}/metrics` | Set demand velocity and reorder threshold; may queue advice if a trigger condition is met. |
| `POST /products/{productId}/orders` | Simulate a sale, reducing stock and increasing demand velocity; may queue advice asynchronously. |
| `POST /products/{productId}/suggest-pricing` | Queue a manual pricing suggestion; returns `202 Accepted`. |
| `POST /products/{productId}/suggest-reorder` | Queue a manual reorder suggestion; returns `202 Accepted`. |
| `POST /products/{productId}/suggest-pricing/stream` | Stream pricing reasoning and then the saved pricing suggestion using SSE. |

### Create product

`POST /products`

```json
{
  "sku": "SKU-001",
  "name": "Wireless Earbuds",
  "category": "ELECTRONICS",
  "currentPrice": 79.99,
  "stockLevel": 45,
  "reorderThreshold": 20
}
```

`currentPrice` must be positive. Stock and threshold must be nonnegative integers.

### Update stock

`PATCH /products/{productId}/stock`

```json
{"stockLevel": 12}
```

### Update demand and reorder threshold

`PATCH /products/{productId}/metrics`

```json
{"demandVelocity": 5, "reorderThreshold": 15}
```

Both values must be nonnegative integers.

### Simulate a sale

`POST /products/{productId}/orders`

```json
{"quantity": 1}
```

Quantity must be positive and cannot exceed available stock.

### Product response

Product endpoints return this shape:

```json
{
  "id": "8ddde8e7-83d2-4b9a-8650-13e99928669b",
  "sku": "SKU-001",
  "name": "Wireless Earbuds",
  "category": "ELECTRONICS",
  "currentPrice": 79.99,
  "stockLevel": 45,
  "reorderThreshold": 20,
  "demandVelocity": 0,
  "status": "ACTIVE"
}
```

## Suggestions

### List suggestions

`GET /suggestions?status=PENDING` returns suggestions sorted newest first. `status` is optional and defaults to `PENDING`.

A `SuggestionResponse` has `id`, `productId`, `productName`, `type`, `status`, `triggerReason`, `confidence`, `reasoning`, and `createdAt`. Pricing suggestions also include `currentPrice`, `recommendedPrice`, and `direction`. Reorder suggestions include `currentStock`, `recommendedQuantity`, and `suggestedLeadTimeDays`; fields not applicable to a suggestion type are `null`.

### Accept or reject

- `PATCH /pricing-suggestions/{suggestionId}`
- `PATCH /reorder-suggestions/{suggestionId}`

Body:

```json
{"decision": "ACCEPTED"}
```

`decision` must be `ACCEPTED` or `REJECTED`. A pricing acceptance updates the product price; a reorder acceptance adds the suggested quantity to stock. Rejection leaves product values unchanged. Decisions apply to `PENDING` suggestions only.

## Pricing SSE

`POST /products/{productId}/suggest-pricing/stream` returns `text/event-stream`. The frontend should send `Accept: text/event-stream` and read the response incrementally.

```text
event: reasoning
data: {"token":"Stock is below threshold..."}

event: suggestion
data: {"id":"...","type":"PRICING","status":"PENDING", ...}
```

The server may send multiple `reasoning` events. It sends one `suggestion` event after the suggestion is persisted, or an `error` event with `{"message":"..."}` if generation cannot complete. The event only creates a proposal; a merchandiser must still accept it before the price changes. AI failures use rule-based pricing advice.

## Advisor mode

- `GET /settings/strategy`
- `PATCH /settings/strategy` with `{"mode":"AI"}`

Response: `{"mode":"AUTO","llmConfigured":false}`. `AUTO` uses AI when `LLM_API_KEY` is configured and otherwise uses rules. The setting is in memory and resets to `AUTO` when the application restarts.

## Errors

Requests may return `400` for invalid input, `404` for an unknown product or suggestion, and `409` for duplicate SKUs or a duplicate pending manual pricing suggestion. `500` indicates an unexpected server error.
