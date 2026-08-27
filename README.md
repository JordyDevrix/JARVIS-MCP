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

#### Dutch Public Transport (OV) Tools

- `GET /v1/tools/ov/locations`
- Real-time GPS locations of public transport vehicles (buses, trams, metros) across the Netherlands.
- Query params:
  - `radius_km` (optional, default `5.0`)
  - `latitude` (optional, defaults to observer location)
  - `longitude` (optional, defaults to observer location)
  - `operator` (optional, e.g. `GVB`, `RET`, `HTM`, `CXX`, `ARR`, `EBS`, `KEOLIS`, `QBUZZ`)
  - `transport_type` (optional, `BUS`, `TRAM`, `METRO`, `ALL`)
  - `line_number` (optional, e.g. `300`, `26`, `51`)
  - `limit` (optional, default `25`)

Example:

```bash
curl "http://localhost:8080/v1/tools/ov/locations?radius_km=5.0&operator=GVB"
```

- `GET /v1/tools/ov/departures`
- Live departure board, delays, platforms, and passes for any Dutch transit stop or station.
- Query params:
  - `stop` (required, stop area code like `schns` or station name like `Schiphol` or `Centraal Station`)
  - `transport_type` (optional, `BUS`, `TRAM`, `METRO`)
  - `limit` (optional, default `20`)

Example:

```bash
curl "http://localhost:8080/v1/tools/ov/departures?stop=schns"
```

- `GET /v1/tools/ov/stops`
- Search ~4,450 Dutch transit hubs, stations, and stops by name/city or geographic proximity.
- Query params:
  - `query` (optional, search term e.g. `Schiphol`, `Utrecht`)
  - `latitude`, `longitude` (optional, coordinates for proximity search)
  - `radius_km` (optional, default `5.0`)
  - `limit` (optional, default `15`)

Example:

```bash
curl "http://localhost:8080/v1/tools/ov/stops?query=Schiphol"
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

#### Dutch OV MCP

- `POST /v1/mcp/ov/locations`
- JSON body:

```json
{ "radiusKm": 5.0, "operator": "GVB" }
```

- `POST /v1/mcp/ov/departures`
- JSON body:

```json
{ "stopAreaCode": "schns" }
```

- `POST /v1/mcp/ov/stops`
- JSON body:

```json
{ "query": "Schiphol" }
```

## Integration Notes

- Tool endpoints are simple REST routes for direct integrations.
- MCP endpoints accept request models and return normalized tool responses.
- OpenAPI docs are available at `GET /docs` when the server is running.

## Code Structure

- `src/main/kotlin/com/clovercloud/jarvis/JarvisApplication.kt`: Spring Boot entry point
- `src/main/kotlin/com/clovercloud/jarvis/config`: Configuration and logging components
- `src/main/kotlin/com/clovercloud/jarvis/clients`: External REST clients (FlightradarClient, OvClient)
- `src/main/kotlin/com/clovercloud/jarvis/services`: Business logic & cache management (FlightradarService, OvService, ToolManagerService)
- `src/main/kotlin/com/clovercloud/jarvis/facades`: Unified facades (OvFacade) decoupling controllers and tools from services
- `src/main/kotlin/com/clovercloud/jarvis/controllers`: HTTP REST controllers and development endpoints
- `src/main/kotlin/com/clovercloud/jarvis/tools`: MCP tool implementations (ClockTools, LocationTools, FlightradarTools, OvTools)
- `src/main/kotlin/com/clovercloud/jarvis/requests`: Request payload models
- `src/main/kotlin/com/clovercloud/jarvis/responses`: Normalized response models
- `src/test/kotlin/com/clovercloud/jarvis`: Test sources
