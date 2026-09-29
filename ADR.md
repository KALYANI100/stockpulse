# StockPulse Architecture Decision Records

These records capture the six core decisions for the initial inventory-advisor milestone. Each decision uses Context, Options, Decision, and Tradeoffs.

## ADR-001: Place Commerce Logic Behind Application Services

**Status:** Accepted

**Context**

Product updates, inventory signals, recommendation generation, and suggestion approvals have different responsibilities. HTTP controllers and asynchronous event handlers must invoke the same recommendation behavior, and domain state transitions must remain enforceable regardless of caller.

**Options**

- Put pricing and reorder rules in REST controllers.
- Accumulate all workflow logic in one large service.
- Use application services to orchestrate domain entities, repositories, advisor strategies, and events.

**Decision**

Controllers validate and translate HTTP requests. `ProductService`, `RecommendationGenerationService`, and `SuggestionReviewService` coordinate use cases. Domain entities such as `Product`, `PricingSuggestion`, and `ReorderSuggestion` own state transitions and basic invariants. Advisor implementations own recommendation logic.

**Tradeoffs**

This adds a few small classes, but keeps event and HTTP paths consistent and makes domain behavior easier to test. The current project is a modular Spring Boot application, not independently deployed services.

## ADR-002: Use One Advisor Contract and One AI Response for Both Suggestions

**Status:** Accepted

**Context**

An inventory signal normally requires both a pricing proposal and a replenishment proposal. Both decisions use the same product, demand, and trigger context. Manual requests can still persist only the requested suggestion type.

**Options**

- Make separate pricing and reorder advisor contracts and issue two LLM calls.
- Use one commerce-advisor contract and one structured AI response containing both recommendations.

**Decision**

Use a unified `AdviceBundle` with pricing and reorder advice. The AI prompt requests both results in one JSON response; the deterministic advisor implements the same output shape. Persist only the suggestion types requested by a manual call, or both for an automatic inventory signal.

**Tradeoffs**

A unified request reduces duplicate context, latency, and provider cost. The output schema couples the two AI results: malformed or invalid output falls back to rules for the bundle. If pricing and replenishment later need different providers, latency budgets, or independent approval lifecycles, split the contract then.

## ADR-003: Select the Advisor at Runtime

**Status:** Accepted

**Context**

Merchandisers and asynchronous signal handlers need the same active recommendation strategy. Local development must work without an LLM credential.

**Options**

- Hard-code one advisor at startup.
- Select an advisor from an environment setting that requires restart to change.
- Keep an in-process strategy setting that can be changed through the API.

**Decision**

`StrategyState` supports `AUTO`, `AI`, and `RULES`. `PATCH /api/settings/strategy` changes the active mode without restarting. `AUTO` uses AI when a key is configured and otherwise uses rules. The same `CommerceAdvisorService` serves manual and event-driven recommendations.

**Tradeoffs**

The setting is simple to change during a demo, but it is held in process memory and resets to `AUTO` after restart. It is not user-specific or persisted; a multi-user deployment would need authorization and durable configuration.

## ADR-004: Validate AI Output and Fall Back to Rules

**Status:** Accepted

**Context**

The LLM gateway can time out, reject a request, return malformed JSON, or propose unsafe values. Inventory-triggered recommendations must not silently disappear when AI is unavailable.

**Options**

- Trust and persist the model response directly.
- Fail the recommendation request when AI fails.
- Validate parsed AI output and use deterministic recommendations whenever the AI path is unavailable or invalid.

**Decision**

Call the configured OpenAI-compatible chat-completions gateway only from the backend. Keep its API key in `LLM_API_KEY`; do not send the cookie from `llm.txt`. Use request timeouts, parse a typed response, validate positive bounded prices, matching price direction, positive bounded reorder quantity, lead time, and confidence in the 0–1 range. On missing credentials, timeout, provider error, parse error, or validation failure, return the rule-based bundle.

**Tradeoffs**

The application remains usable without external connectivity and all stored suggestions satisfy local constraints. Rule results are less context-aware than AI advice, and the initial bounds are product-agnostic until margin floors and category-specific rules are implemented.

## ADR-005: Trigger Recommendations After Inventory Commit

**Status:** Accepted

**Context**

Stock updates and simulated orders should return promptly. Recommendation generation may involve a slow external service. Repeated low-stock or demand-spike signals must not create duplicate pending suggestions.

**Options**

- Generate recommendations synchronously in the stock/order request.
- Poll inventory on a timer.
- Publish an application event after commit and handle it asynchronously.

**Decision**

Stock and order use cases publish `RecommendationRequestedEvent`. A `@TransactionalEventListener` handles committed events on the recommendation executor. Before writing, generation checks for a pending suggestion with the same product, trigger reason, and suggestion type. Suggestions remain proposals; acceptance is the human checkpoint for price or stock changes.

**Tradeoffs**

The request path stays fast and business logic is shared with manual requests. The in-process event is not durable: a process crash can lose queued work, and the duplicate check is not a distributed lock. If delivery guarantees or multiple backend instances are required, add an outbox or durable broker and database-level idempotency.

## ADR-006: Keep the Initial Scope Extensible but Small

**Status:** Accepted

**Context**

The initial goal is the inventory-signal → recommendation → human-approval loop. Future work may add competitor prices, margin policies, supplier catalogs, purchase orders, and storefront integration.

**Options**

- Implement competitor ingestion, automatic pricing, and purchasing immediately.
- Build only the current loop while preserving domain and advisor extension points.
- Design and deploy separate microservices and databases before the workflow is proven.

**Decision**

Keep product/inventory, recommendation, and suggestion/approval responsibilities in separate Spring packages within one application. Use advisor implementations behind a common contract, persist inventory snapshots, and leave later product fields such as `costPrice`, `marginFloor`, and `supplierId` for the iteration that uses them. Defer competitor scraping, automatic price publication, automated purchase orders, authentication, durable messaging, and separate service deployments.

**Tradeoffs**

This keeps local setup simple, requires no external database or broker, and avoids speculative schema. The initial boundaries are logical rather than network-isolated; future service extraction will require API contracts, per-service data ownership, authentication, and reliable event delivery.
