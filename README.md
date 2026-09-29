# StockPulse

StockPulse is an inventory and dynamic-pricing advisor for merchandising teams. It detects low-stock and demand-spike signals, generates pricing and reorder suggestions, and waits for human approval before applying changes.

## Project

- Backend: Java 21, Spring Boot, JPA, local in-memory H2
- Frontend: React, TypeScript, Vite
- Recommendations: AI gateway when configured; rule-based fallback otherwise

## Contents

- [Local run and seed commands](command.md)
- [Architecture and diagrams](architecture.md)
- [Use cases](README-use-cases.md)
- [Architecture decision records](ADR.md)
- [API reference](docs/api/api.md)
- [Inventory-low prompt](docs/prompts/inventory-low.md)
- [Demand-spike prompt](docs/prompts/demand-spike.md)
