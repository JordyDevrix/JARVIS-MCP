# JARVIS MCP

JARVIS MCP is a Kotlin and Spring Boot service that exposes clock and location tools over HTTP and MCP endpoints, with version tracking and deployment metadata.

## Requirements

- Java 25
- Maven Wrapper (`./mvnw`)

## Setup and Run

### Local

```bash
./mvnw test
./mvnw spring-boot:run
```

Default base URL: `http://localhost:8080`

### Docker Compose

```bash
docker compose up -d
```

The compose file publishes `${SERVER_PORT:-8080}` to container port `8080`.

## API Overview

### Health and Version

- `GET /v1/development/health`
- Response: plain text `Healthy`

- `GET /v1/development/version`
- Response: JSON with version, gitCommit, buildTime metadata

```bash
curl "http://localhost:8080/v1/development/version"
```

### Tool Endpoints

#### Clock Tool

- `GET /v1/tools/clock/time`
- Query params:
  - `timezone` (optional, default `UTC`)

Example:

```bash
curl "http://localhost:8080/v1/tools/clock/time?timezone=Europe/Amsterdam"
```

#### Location Tool

- `GET /v1/tools/location/current`
- Returns the default mock location (Amsterdam)

- `GET /v1/tools/location/lookup`
- Query params:
  - `city` (optional, default `Amsterdam`)

Example:

```bash
curl "http://localhost:8080/v1/tools/location/lookup?city=Tokyo"
```

### MCP Endpoints

#### Clock MCP

- `POST /v1/mcp/clock`
- JSON body:

```json
{ "timezone": "Europe/Amsterdam" }
```

Example:

```bash
curl -X POST "http://localhost:8080/v1/mcp/clock" \
  -H "Content-Type: application/json" \
  -d '{"timezone":"Europe/Amsterdam"}'
```

#### Location MCP

- `POST /v1/mcp/location`
- JSON body:

```json
{ "city": "London" }
```

Example:

```bash
curl -X POST "http://localhost:8080/v1/mcp/location" \
  -H "Content-Type: application/json" \
  -d '{"city":"London"}'
```

## Integration Notes

- Tool endpoints are simple REST routes for direct integrations.
- MCP endpoints accept request DTOs and return normalized tool responses.
- OpenAPI docs are available at `GET /docs` when the server is running.

## Code Structure

- `src/main/kotlin/com/clovercloud/jarvis/JarvisApplication.kt`: Spring Boot entry point
- `src/main/kotlin/com/clovercloud/jarvis/config`: Configuration and logging components
- `src/main/kotlin/com/clovercloud/jarvis/controllers`: HTTP REST controllers and development endpoints
- `src/main/kotlin/com/clovercloud/jarvis/tools`: MCP tool implementations (ClockTools, LocationTools)
- `src/main/kotlin/com/clovercloud/jarvis/requests`: MCP request DTOs
- `src/main/kotlin/com/clovercloud/jarvis/responses`: Response DTOs
- `src/test/kotlin/com/clovercloud/jarvis`: Test sources
