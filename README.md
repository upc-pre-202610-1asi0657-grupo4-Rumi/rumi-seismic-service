# rumi-seismic-service

Seismic Correlation service of **Rumi**, the structural monitoring platform by Kuntur Labs.

| | |
|---|---|
| Bounded context | Seismic Correlation (includes the risk index, formerly the `riskassessment` package) |
| Port | `8083` |
| Database | `seismiceventsdb` (PostgreSQL), reserved, not connected yet |
| Messaging | RabbitMQ, subscriber |
| Gateway routes | `/api/v1/seismic-events/**`, `/api/v1/risk-indexes/**`, `/api/v1/reports/**`, `/api/v1/buildings/{buildingId}/risk-indexes/**` |
| Base package | `com.rumi.seismiccorrelation` |

## Purpose

Correlates the seismic events reported by the IGP (Instituto Geofisico del Peru) with the
structural response of each building and calculates its risk index.

- IGP integration: `IgpFeedClient` behind a Resilience4j circuit breaker (`igpFeed`).
- Risk index: `RiskIndexCalculationService` with the Strategy pattern
  (`RiskCalculationStrategy`, `AiRiskStrategy`, `RuleBasedRiskStrategy`).

## Origin

Extracted from the modular monolith
[`rumi-backend`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-backend) at commit
`rumi-backend@3ab07ec` (branch `develop`), with the history of this context preserved.
The state of the monolith before the decomposition is the tag
[`monolith-baseline`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-backend/tree/monolith-baseline).

## Endpoints

| Verb | Path | Description | Request | Response | User story | Status |
|---|---|---|---|---|---|---|
| none | | No REST endpoint is implemented yet | | | | |

Implemented functional endpoints: 0. The seismic events (US13), risk index (US11, US27)
and report (US12, US23) endpoints are planned for later sprints; the domain and integration
code behind US11 and US13 already exists but is not exposed over REST.

## Events

| Event | Direction | Exchange | Routing key | Queue |
|---|---|---|---|---|
| `SensorReadingRecorded` | consumed | `rumi.structural-monitoring.events` (topic, durable) | `structural-monitoring.sensor-reading.recorded` | `rumi.seismic-correlation.sensor-reading-recorded` (durable) |

```json
{
  "eventId": "7c1f5b0e-3a52-4d0b-9f5e-2f6c1a8f4d11",
  "sensorId": "b2a9d0f4-6c1e-4a57-8f0a-91d2c3e4f5a6",
  "buildingId": "3f2c8a10-5d7b-4e9a-b1c2-0a1b2c3d4e5f",
  "recordedAt": "2026-10-06T15:30:00Z",
  "value": 0.018
}
```

The contract class is `com.rumi.seismiccorrelation.domain.event.SensorReadingRecorded`, this
service's own copy. The publisher (`rumi-monitoring-service`) keeps its own; a change must be
applied on both sides.

## API documentation

- Swagger UI: <http://localhost:8083/swagger-ui.html>
- OpenAPI spec: <http://localhost:8083/v3/api-docs>
- Exported spec: [`docs/openapi.json`](docs/openapi.json) (no paths yet)

## Run

Requirements: JDK 21, Maven. RabbitMQ is needed to receive events; without it the service
starts and keeps retrying the connection.

```sh
mvn spring-boot:run
```

| Variable | Default |
|---|---|
| `SERVER_PORT` | `8083` |
| `RABBITMQ_HOST` | `localhost` |
| `RABBITMQ_PORT` | `5672` |
| `RABBITMQ_USERNAME` | `guest` |
| `RABBITMQ_PASSWORD` | `guest` |
| `SEISMIC_DB_URL` | `jdbc:postgresql://localhost:5432/seismiceventsdb` (reserved) |
| `SEISMIC_DB_USERNAME` | `postgres` (reserved) |
| `SEISMIC_DB_PASSWORD` | `postgres` (reserved) |

## Test

```sh
mvn test
```

The tests need neither a database nor a message broker.

## Structure

```
com.rumi.seismiccorrelation
├── application                  SensorReadingRecordedHandler
├── domain
│   ├── event                    SensorReadingRecorded contract
│   ├── model                    SeismicEvent
│   ├── service                  RiskIndexCalculationService
│   └── strategy                 risk calculation strategies
└── infrastructure
    ├── external.igp             IGP feed client and mapper
    ├── messaging.rabbitmq       subscriber and RabbitMQ configuration
    └── web                      OpenAPI configuration
```

## Known limitations

- `SensorReadingRecordedHandler` only logs the received event.
- The risk index classes are plain domain objects, not yet wired into a use case.
- `AiRiskStrategy` is a deterministic placeholder until a trained model is integrated.
