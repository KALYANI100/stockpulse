# StockPulse Architecture

## Logical Services

| Service or component | Responsibility |
| --- | --- |
| React Merchandising Console | Display products and pending suggestions; simulate sales and stock changes; request advice; accept or reject suggestions. |
| API Gateway / Backend API | Expose REST endpoints to React, validate requests, and route commands to backend services. In the initial version this is the Spring Boot web/API layer, not a separate gateway deployment. |
| Product and Inventory Service | Own product details, stock, simulated orders, inventory snapshots, and low-stock or demand-spike signal detection. |
| Recommendation Service | Consume inventory signals, prevent duplicate pending work, select AI or rule-based strategy, and request pricing and reorder advice. |
| Suggestion and Approval Service | Store pending pricing/reorder suggestions and their lifecycle; apply accepted price or simulated stock changes through the Product and Inventory Service. |
| LLM Adapter | Backend-only connector to the configured OpenAI-compatible gateway; handles request mapping, timeout, response parsing, and validation. It is a component of the Recommendation Service initially, not a separate deployed service. |
| H2 Database | Local SQL database for the initial application. For independently deployed services, give each service ownership of its own database; do not have services write to another service's tables. |
| External LLM Gateway | Supplies model responses to the LLM Adapter. The API key stays on the backend and is read from an environment variable. |

**Deployment note:** The generated project currently contains a single Spring Boot application and H2 dependency, not independently deployed microservices. Build the boundaries as modules inside that application first, using Spring application events. Split into networked services and add a durable message broker only when independent deployment or scaling is needed.

## Component Diagram

```mermaid
flowchart LR
    User[Merchandiser] --> UI[React Merchandising Console]
    UI -->|REST: catalog, stock, orders, suggestions, decisions| API[Spring Boot API]

    subgraph Backend[StockPulse backend: logical service boundaries]
        API --> INVENTORY[Product and Inventory Service]
        API --> SUGGESTION[Suggestion and Approval Service]
        API --> REC[Recommendation Service]
        INVENTORY -->|post-commit InventorySignal| EVENTS[Spring Event Bus initially]
        EVENTS -->|async signal| REC
        REC -->|request product context| INVENTORY
        REC -->|persist pending advice| SUGGESTION
        SUGGESTION -->|accepted price or inbound stock| INVENTORY
        REC --> STRATEGY[Commerce Strategy Selector]
        STRATEGY --> RULES[Rule-based Advisor]
        STRATEGY --> AI[AI Advisor]
        AI --> ADAPTER[LLM Adapter]
    end

    INVENTORY --> DB[(Local H2 for initial version)]
    SUGGESTION --> DB
    ADAPTER -->|HTTPS chat completions| LLM[External LLM Gateway]
```

## Use-Case Diagram

```mermaid
flowchart LR
    Merchandiser((Merchandiser))
    Signal((Inventory or order change))
    Provider((LLM gateway))

    subgraph App[StockPulse]
        Catalog([View product and stock])
        ChangeStock([Adjust stock or simulate sale])
        Detect([Detect low stock or demand spike])
        Manual([Request pricing or reorder advice])
        Generate([Generate pricing and reorder suggestions])
        Review([Review pending suggestions and reasoning])
        Decide([Accept or reject suggestion])
        ApplyPrice([Apply accepted price])
        ApplyStock([Apply accepted reorder quantity])
    end

    Merchandiser --> Catalog
    Merchandiser --> ChangeStock
    Merchandiser --> Manual
    Merchandiser --> Review
    Merchandiser --> Decide
    ChangeStock --> Detect
    Signal --> Detect
    Detect --> Generate
    Manual --> Generate
    Generate --> Provider
    Generate --> Review
    Decide --> ApplyPrice
    Decide --> ApplyStock
```

## Class Diagram

```mermaid
classDiagram
    class Product {
        +UUID id
        +String sku
        +String name
        +Category category
        +BigDecimal currentPrice
        +int stockLevel
        +int reorderThreshold
        +int demandVelocity
        +ProductStatus status
    }
    class InventorySnapshot {
        +UUID id
        +int stockLevel
        +int demandVelocity
        +Instant capturedAt
    }
    class InventorySignal {
        +UUID productId
        +TriggerReason reason
        +Instant occurredAt
    }
    class PricingSuggestion {
        +UUID id
        +BigDecimal currentPrice
        +BigDecimal recommendedPrice
        +PriceDirection direction
        +double confidence
        +String reasoning
        +SuggestionStatus status
        +TriggerReason triggerReason
        +decide(SuggestionStatus decision)
    }
    class ReorderSuggestion {
        +UUID id
        +int currentStock
        +int recommendedQuantity
        +int suggestedLeadTimeDays
        +double confidence
        +String reasoning
        +SuggestionStatus status
        +TriggerReason triggerReason
        +decide(SuggestionStatus decision)
    }
    class RecommendationContext {
        +Product product
        +double categoryAverageVelocity
        +TriggerReason triggerReason
    }
    class CommerceAdvisor {
        <<interface>>
        +advise(RecommendationContext) AdviceBundle
    }
    class AiCommerceAdvisor {
        +advise(RecommendationContext) AdviceBundle
    }
    class RuleBasedCommerceAdvisor {
        +advise(RecommendationContext) AdviceBundle
    }
    class RecommendationService {
        +onInventorySignal(InventorySignal)
        +requestManualAdvice(UUID productId)
    }
    class SuggestionApprovalService {
        +acceptPricing(UUID suggestionId)
        +rejectPricing(UUID suggestionId)
        +acceptReorder(UUID suggestionId)
        +rejectReorder(UUID suggestionId)
    }
    class LLMAdapter {
        +complete(String prompt) String
    }

    Product "1" --> "0..*" InventorySnapshot : history
    Product "1" --> "0..*" PricingSuggestion : advice
    Product "1" --> "0..*" ReorderSuggestion : advice
    RecommendationService --> CommerceAdvisor : selects
    CommerceAdvisor <|.. AiCommerceAdvisor
    CommerceAdvisor <|.. RuleBasedCommerceAdvisor
    AiCommerceAdvisor --> LLMAdapter : calls
    RecommendationService --> RecommendationContext : builds
    RecommendationService --> PricingSuggestion : creates
    RecommendationService --> ReorderSuggestion : creates
    SuggestionApprovalService --> PricingSuggestion : decides
    SuggestionApprovalService --> ReorderSuggestion : decides
    SuggestionApprovalService --> Product : applies accepted change
```

## Activity Diagram: Signal to Human Decision

```mermaid
flowchart TD
    Start((Start)) --> Change[Stock update or sale is recorded]
    Change --> Commit[Commit inventory change]
    Commit --> Trigger{Low stock or demand spike?}
    Trigger -->|No| Done[Return; no automatic advice]
    Trigger -->|Yes| Publish[Publish inventory signal after commit]
    Publish --> Async[Handle signal asynchronously]
    Async --> Duplicate{Matching pending suggestion exists?}
    Duplicate -->|Yes| Skip[Skip duplicate generation]
    Duplicate -->|No| Context[Load product and category context]
    Context --> Strategy[Select active commerce advisor]
    Strategy --> AI[Request AI pricing and reorder advice]
    AI --> Valid{Response valid and within bounds?}
    Valid -->|Yes| UseAI[Use AI recommendations]
    Valid -->|No or timeout| Fallback[Use rule-based recommendations]
    UseAI --> Persist[Persist both suggestions as PENDING]
    Fallback --> Persist
    Persist --> Display[Console shows suggestions and trigger reason]
    Display --> Decision{Merchandiser decision}
    Decision -->|Reject| Reject[Mark suggestion REJECTED; product unchanged]
    Decision -->|Accept pricing| Price[Mark ACCEPTED and update current price]
    Decision -->|Accept reorder| Stock[Mark ACCEPTED and increase stock]
    Done --> End((End))
    Skip --> End
    Reject --> End
    Price --> End
    Stock --> End
```

## State and Ownership Rules

- Inventory or order changes commit before their signal is handled asynchronously.
- The recommendation service creates proposals; it never publishes prices or places orders.
- Suggestions transition from `PENDING` to `ACCEPTED` or `REJECTED` exactly once.
- Only an accepted pricing suggestion changes `Product.currentPrice`; only an accepted reorder suggestion increases stock.
- AI failure or invalid output uses the rule-based advisor. Repeated events must not create duplicate pending suggestions for the same product, trigger reason, and suggestion type.
