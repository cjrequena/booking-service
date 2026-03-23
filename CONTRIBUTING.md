# Contributing to Booking Service

Thank you for your interest in contributing! This guide will help you get started.

## Prerequisites

- **Java 21** (we recommend [SDKMAN](https://sdkman.io/) for managing JDK versions)
- **Maven 3.9+**
- **Docker & Docker Compose**
- **Git**

## Getting Started

```bash
git clone https://github.com/cjrequena/booking-service.git
cd booking-service
```

### Start Infrastructure

```bash
cd .docker
docker-compose up -d postgres mongo kafka redis
cd ..
```

### Build & Test

```bash
mvn clean install
```

### Run Integration Tests

Integration tests use [Testcontainers](https://testcontainers.com/) — Docker must be running:

```bash
mvn verify -pl booking-command-handler
```

### Run the Application

```bash
# Terminal 1 — Command Handler
cd booking-command-handler && mvn spring-boot:run

# Terminal 2 — Query Handler
cd booking-query-handler && mvn spring-boot:run
```

### Verify

```bash
curl http://localhost:8080/management/healthcheck
curl http://localhost:8081/management/healthcheck
```

## API Headers

The API uses header-based versioning. The correct headers are:

| Service | Header |
|---------|--------|
| Command Handler | `Accept-Version: application/vnd.booking-command-handler.v1` |
| Query Handler | `Accept-Version: application/vnd.booking-query-handler.v1` |

> **Note:** Do NOT append `+json` to the header value. The header must match exactly or the request will return a 404.

## Project Structure

```
booking-service/
├── es-core/                    # Event Sourcing core library
├── booking-command-handler/    # Write side (commands, events, event store)
├── booking-query-handler/      # Read side (projections, queries)
├── .docker/                    # Docker Compose + provisioning scripts
└── .docs/                      # Architecture and specification docs
```

## Adding a New Product Type

The system uses a polymorphic product model. To add a new product type (e.g., `Activity`):

1. Add `ACTIVITY("Activity")` to `ProductType` enum (both modules)
2. Add `public static final String ACTIVITY = "Activity"` to `Constant` (both modules)
3. Create `domain/model/vo/activity/` with your VOs implementing `ProductVO`
4. Create `persistence/mongodb/entity/activity/` with entities extending `ProductEntity`
5. Register in `@JsonSubTypes` on `ProductVO` interface and `ProductEntity` class (both modules)
6. Add mapping method and switch case in `ProductMapper` (both modules)

No changes needed in aggregates, commands, events, controllers, or projections — the polymorphic design handles it.

## Code Style

- Follow the existing patterns — records for VOs, Lombok for entities
- Use `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` on all DTOs/VOs/entities
- Immutable value objects with validation in canonical constructors
- Keep domain logic in the aggregate, not in services

## Testing

- **Unit tests**: `src/test/java/` — run with `mvn test`
- **Integration tests**: Files ending in `*IT.java` — run with `mvn verify`
- Don't break existing tests — run the full suite before submitting a PR

## Submitting Changes

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Make your changes with clear, atomic commits
4. Run `mvn clean verify` to ensure all tests pass
5. Open a Pull Request with a description of what and why

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add Activity product type
fix: correct header validation in controller
docs: update API examples in README
test: add integration test for multi-product booking
```

## Questions?

Open an issue on GitHub for bugs, feature requests, or questions.
