# StockPulse API Documentation

This document provides detailed information about the StockPulse REST API endpoints.

## Base URL

```
http://localhost:8080/api
```

## Authentication

No authentication is required for the API endpoints in the current version.

## Error Handling

The API uses standard HTTP status codes to indicate the success or failure of requests:

- `200 OK` - Success
- `201 Created` - Resource created successfully
- `202 Accepted` - Request accepted for processing
- `400 Bad Request` - Invalid request data
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

## Products

### List Products

Get a list of all products with optional filtering.

```
GET /products
```

#### Query Parameters

| Parameter | Type     | Required | Description                    |
|-----------|----------|----------|--------------------------------|
| category  | Category | No       | Filter by product category     |
| status    | ProductStatus | No  | Filter by product status       |

#### Response

```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "sku": "string",
    "name": "string",
    "category": "ELECTRONICS",
    "currentPrice": 0,
    "stockLevel": 0,
    "reorderThreshold": 0,
    "demandVelocity": 0,
    "status": "ACTIVE"
  }
]
```

#### Example

```bash
curl -X GET "http://localhost:8080/api/products?category=ELECTRONICS&status=ACTIVE"
```

### Get Product

Get details of a specific product by ID.

```
GET /products/{productId}
```

#### Path Parameters

| Parameter  | Type | Required | Description         |
|------------|------|----------|---------------------|
| productId  | UUID | Yes      | ID of the product   |

#### Response

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "sku": "string",
  "name": "string",
  "category": "ELECTRONICS",
  "currentPrice": 0,
  "stockLevel": 0,
  "reorderThreshold": 0,
  "demandVelocity": 0,
  "status": "ACTIVE"
}
```

#### Example

```bash
curl -X GET "http://localhost:8080/api/products/3fa85f64-5717-4562-b3fc-2c963f66afa6"
```

### Create Product

Create a new product.

```
POST /products
```

#### Request Body

```json
{
  "sku": "string",
  "name": "string",
  "category": "ELECTRONICS",
  "initialPrice": 0,
  "initialStock": 0,
  "reorderThreshold": 0,
  "demandVelocity": 0
}
```

#### Response

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "sku": "string",
  "name": "string",
  "category": "ELECTRONICS",
  "currentPrice": 0,
  "stockLevel": 0,
  "reorderThreshold": 0,
  "demandVelocity": 0,
  "status": "ACTIVE"
}
```

#### Example

```bash
curl -X POST "http://localhost:8080/api/products" \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "PROD-001",
    "name": "Sample Product",
    "category": "ELECTRONICS",
    "initialPrice": 29.99,
    "initialStock": 100,
    "reorderThreshold": 20,
    "demandVelocity": 5
  }'
```

### Update Stock Level

Update the stock level of a product.

```
PATCH /products/{productId}/stock
```

#### Path Parameters

| Parameter  | Type | Required | Description         |
|------------|------|----------|---------------------|
| productId  | UUID | Yes      | ID of the product   |

#### Request Body

```json
{
  "stockLevel": 0
}
```

#### Response

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "sku": "string",
  "name": "string",
  "category": "ELECTRONICS",
  "currentPrice": 0,
  "stockLevel": 0,
  "reorderThreshold": 0,
  "demandVelocity": 0,
  "status": "ACTIVE"
}
```

#### Example

```bash
curl -X PATCH "http://localhost:8080/api/products/3fa85f64-5717-4562-b3fc-2c963f66afa6/stock" \
  -H "Content-Type: application/json" \
  -d '{"stockLevel": 75}'
```

### Update Product Metrics

Update the demand velocity and reorder threshold of a product.

```
PATCH /products/{productId}/metrics
```

### Request Pricing Suggestion

Request a pricing suggestion for a product.

```
POST /products/{productId}/suggest-pricing
```

#### Path Parameters

| Parameter  | Type | Required | Description         |
|------------|------|----------|---------------------|
| productId  | UUID | Yes      | ID of the product   |

#### Response

```http
HTTP/1.1 202 Accepted
```

#### Example

```bash
curl -X POST "http://localhost:8080/api/products/3fa85f64-5717-4562-b3fc-2c963f66afa6/suggest-pricing"
```

### Request Pricing Suggestion Stream

Request a pricing suggestion with streaming response that includes reasoning.

```
POST /products/{productId}/suggest-pricing/stream
```

#### Path Parameters

| Parameter  | Type | Required | Description         |
|------------|------|----------|---------------------|
| productId  | UUID | Yes      | ID of the product   |

## Suggestions

### List Suggestions

Get a list of suggestions with optional filtering by status.

```
GET /suggestions
```

#### Query Parameters

| Parameter | Type     | Required | Description                    |
|-----------|----------|----------|--------------------------------|
| status    | SuggestionStatus | No  | Filter by suggestion status (default: PENDING) |

#### Response

```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "productId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "productName": "string",
    "type": "PRICING",
    "status": "PENDING",
    "triggerReason": "INVENTORY_LOW",
    "confidence": 0,
    "reasoning": "string",
    "currentPrice": 0,
    "recommendedPrice": 0,
    "direction": "UP",
    "currentStock": 0,
    "recommendedQuantity": 0,

### Decide on Pricing Suggestion

Accept or reject a pricing suggestion.

```
PATCH /pricing-suggestions/{suggestionId}
```

#### Path Parameters

| Parameter    | Type | Required | Description             |
|--------------|------|----------|-------------------------|
| suggestionId | UUID | Yes      | ID of the suggestion    |

#### Request Body

```json
{
  "decision": "ACCEPT"
}
```

#### Response

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "productId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "productName": "string",
  "type": "PRICING",
  "status": "ACCEPTED",
  "triggerReason": "INVENTORY_LOW",
  "confidence": 0,
  "reasoning": "string",
  "currentPrice": 0,
  "recommendedPrice": 0,
  "direction": "UP",
  "currentStock": 0,
  "recommendedQuantity": 0,

## Strategy Settings

### Get Strategy

Get the current strategy settings.

```
GET /settings/strategy
```

#### Response

```json
{
  "mode": "AUTO",
  "llmAvailable": true
}
```

#### Example

```bash
curl -X GET "http://localhost:8080/api/settings/strategy"
```

### Update Strategy

Update the strategy settings.


## Enums

### Category

```java
public enum Category {
    ELECTRONICS,
    APPAREL,
    HOME
}
```

### ProductStatus

```java
public enum ProductStatus {
    ACTIVE,
    PRICE_REVIEW_PENDING,
    OUT_OF_STOCK
}
```

### SuggestionStatus

```java
public enum SuggestionStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
```

### SuggestionType

```java
public enum SuggestionType {
    PRICING,
    REORDER
}
```

### TriggerReason

```java
public enum TriggerReason {
    INVENTORY_LOW,
    DEMAND_SPIKE
}
```

### PriceDirection

```java
public enum PriceDirection {
    UP,
    DOWN,
    MAINTAIN
}
```

### StrategyMode

```java
public enum StrategyMode {
    AUTO,
    AI,
    RULES
}
```
```
PATCH /settings/strategy
```

#### Request Body

```json
{
  "mode": "AI"
}
```

#### Response

```json
{
  "mode": "AI",
  "llmAvailable": true
}
```

#### Example

```bash
curl -X PATCH "http://localhost:8080/api/settings/strategy" \
  -H "Content-Type: application/json" \
  -d '{"mode": "AI"}'
```
  "suggestedLeadTimeDays": 0,
  "createdAt": "2023-01-01T00:00:00Z"
}
```

#### Example

```bash
curl -X PATCH "http://localhost:8080/api/pricing-suggestions/3fa85f64-5717-4562-b3fc-2c963f66afa6" \
  -H "Content-Type: application/json" \
  -d '{"decision": "ACCEPT"}'
```

### Decide on Reorder Suggestion

Accept or reject a reorder suggestion.

```
PATCH /reorder-suggestions/{suggestionId}
```

#### Path Parameters

| Parameter    | Type | Required | Description             |
|--------------|------|----------|-------------------------|
| suggestionId | UUID | Yes      | ID of the suggestion    |

#### Request Body

```json
{
  "decision": "ACCEPT"
}
```

#### Response

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "productId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "productName": "string",
  "type": "REORDER",
  "status": "ACCEPTED",
  "triggerReason": "INVENTORY_LOW",
  "confidence": 0,
  "reasoning": "string",
  "currentPrice": 0,
  "recommendedPrice": 0,
  "direction": "UP",
  "currentStock": 0,
  "recommendedQuantity": 0,
  "suggestedLeadTimeDays": 0,
  "createdAt": "2023-01-01T00:00:00Z"
}
```

#### Example

```bash
curl -X PATCH "http://localhost:8080/api/reorder-suggestions/3fa85f64-5717-4562-b3fc-2c963f66afa6" \
  -H "Content-Type: application/json" \
  -d '{"decision": "ACCEPT"}'
```
    "suggestedLeadTimeDays": 0,
    "createdAt": "2023-01-01T00:00:00Z"
  }
]
```

#### Example

```bash
curl -X GET "http://localhost:8080/api/suggestions?status=PENDING"
```

#### Response (Server-Sent Events)

Streams events with reasoning and final suggestion.

#### Example

```bash
curl -X POST "http://localhost:8080/api/products/3fa85f64-5717-4562-b3fc-2c963f66afa6/suggest-pricing/stream"
```

### Request Reorder Suggestion

Request a reorder suggestion for a product.

```
POST /products/{productId}/suggest-reorder
```

#### Path Parameters

| Parameter  | Type | Required | Description         |
|------------|------|----------|---------------------|
| productId  | UUID | Yes      | ID of the product   |

#### Response

```http
HTTP/1.1 202 Accepted
```

#### Example

```bash
curl -X POST "http://localhost:8080/api/products/3fa85f64-5717-4562-b3fc-2c963f66afa6/suggest-reorder"
```

#### Path Parameters

| Parameter  | Type | Required | Description         |
|------------|------|----------|---------------------|
| productId  | UUID | Yes      | ID of the product   |

#### Request Body

```json
{
  "demandVelocity": 0,
  "reorderThreshold": 0
}
```

#### Response

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "sku": "string",
  "name": "string",
  "category": "ELECTRONICS",
  "currentPrice": 0,
  "stockLevel": 0,
  "reorderThreshold": 0,
  "demandVelocity": 0,
  "status": "ACTIVE"
}
```

#### Example

```bash
curl -X PATCH "http://localhost:8080/api/products/3fa85f64-5717-4562-b3fc-2c963f66afa6/metrics" \
  -H "Content-Type: application/json" \
  -d '{"demandVelocity": 10, "reorderThreshold": 30}'
```

### Simulate Sale

Simulate a sale of a product (reduces stock level).

```
POST /products/{productId}/orders
```

#### Path Parameters

| Parameter  | Type | Required | Description         |
|------------|------|----------|---------------------|
| productId  | UUID | Yes      | ID of the product   |

#### Request Body

```json
{
  "quantity": 0
}
```

#### Response

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "sku": "string",
  "name": "string",
  "category": "ELECTRONICS",
  "currentPrice": 0,
  "stockLevel": 0,
  "reorderThreshold": 0,
  "demandVelocity": 0,
  "status": "ACTIVE"
}
```

#### Example

```bash
curl -X POST "http://localhost:8080/api/products/3fa85f64-5717-4562-b3fc-2c963f66afa6/orders" \
  -H "Content-Type: application/json" \
  -d '{"quantity": 5}'
```