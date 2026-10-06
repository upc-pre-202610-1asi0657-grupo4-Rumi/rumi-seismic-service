# rumi-seismic-service

Seismic Correlation service of **Rumi**, the structural monitoring platform by Kuntur Labs.

| | |
|---|---|
| Bounded context | Seismic Correlation (includes the risk index, formerly the `riskassessment` package) |
| Port | `8083` |
| Database | `seismiceventsdb` (PostgreSQL); H2 in memory with the `dev` profile |
| Messaging | RabbitMQ, subscriber |
| Gateway routes | `/api/v1/seismic-events/**`, `/api/v1/risk-indexes/**`, `/api/v1/reports/**`, `/api/v1/buildings/{buildingId}/risk-indexes/**` |
| Base package | `com.rumi.seismiccorrelation` |

## Purpose

Correlates the seismic events reported by the IGP (Instituto Geofisico del Peru) with the
structural response of each building and calculates its risk index.

- IGP integration: `IgpFeedClient` behind a Resilience4j circuit breaker and time limiter (`igpFeed`).
- Correlation: `SeismicCorrelationApplicationService` matches the readings received from
  `rumi-monitoring-service` with the IGP events.
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
| GET | `/api/v1/seismic-events/latest` | Latest event from the IGP, stored if new; falls back to the latest stored event | none | 200 seismic event, 503 if the IGP fails and nothing is stored | US13 | Implemented |
| GET | `/api/v1/seismic-events` | Stored events, newest first | `from`, `to` (ISO-8601, optional, inclusive) | 200 list of seismic events | US13 | Implemented |
| GET | `/api/v1/buildings/{buildingId}/risk-indexes/latest` | Latest risk index of a building | path UUID | 200 risk index, 404 if the building has none | US11 | Implemented |
| GET | `/api/v1/risk-indexes/{riskIndexId}/affected-zones` | Affected zones of a risk index by severity | path UUID | 200 list of zones (`[]` below MEDIUM), 404 if unknown | US12 | Implemented |

Production endpoints implemented: 4. Risk index history (US27) and reports (US23) are planned for
later sprints.

Errors are RFC 7807 `ProblemDetail` (`application/problem+json`):

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Building 0f1e2d3c-4b5a-4968-8776-5a4b3c2d1e0f has no risk index",
  "instance": "/api/v1/buildings/0f1e2d3c-4b5a-4968-8776-5a4b3c2d1e0f/risk-indexes/latest"
}
```

Examples:

```json
// GET /api/v1/seismic-events/latest (and every item of GET /api/v1/seismic-events)
{ "id": "1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9",
  "occurredAt": "2026-10-06T15:29:41Z",
  "magnitude": { "value": 5.8, "scale": "Mw" },
  "epicenter": { "latitude": -12.05, "longitude": -77.12, "description": "Callao, 35 km SW" },
  "source": "IGP" }

// GET /api/v1/buildings/7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d/risk-indexes/latest
{ "id": "c3a1e8d2-6b7f-4d09-9a21-5e8f7b6c4d3a",
  "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
  "correlatedEventId": "9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4",
  "level": "HIGH", "calculatedAt": "2026-10-06T15:32:10Z", "modelVersion": "rule-based-v1" }

// GET /api/v1/risk-indexes/c3a1e8d2-6b7f-4d09-9a21-5e8f7b6c4d3a/affected-zones
[ { "zone": "FLOOR-3-NORTH", "severityRank": 1 }, { "zone": "FLOOR-2-NORTH", "severityRank": 2 } ]
```

### Sample data (dev profile)

The dev profile loads, for the sample building `7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d`, the data used
by the examples above: seismic event `1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9`, correlation
`9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4`, risk index `c3a1e8d2-6b7f-4d09-9a21-5e8f7b6c4d3a` (HIGH) and the
affected zones `FLOOR-3-NORTH` and `FLOOR-2-NORTH`. The stub IGP feed always reports that same event,
so `GET /api/v1/seismic-events/latest` returns it without storing a duplicate.

| Property (dev) | Default | Effect |
|---|---|---|
| `rumi.dev.sample-data` | `true` | `false` starts with an empty database |
| `rumi.igp.stub.fail` | `false` | `true` makes the stub feed fail: the latest stored event is returned, or 503 if there is none |

## Seismic events and the IGP

- Deduplication: an event with the same `occurredAt`, magnitude and epicenter (latitude and longitude)
  as a stored one is the same event. It is checked before storing and enforced by the unique
  constraint `uk_seismic_events_occurrence`.
- Storage follows `seismic_events` of the ER model: generated UUID id, `occurred_at`,
  `magnitude_value`, `magnitude_scale` (`Mw` when the feed gives none), `epicenter_latitude`,
  `epicenter_longitude`, `epicenter_description`, `source = "IGP"`. The depth is not stored.
- Circuit breaker `igpFeed` (QA-05): each call has a **5 s** time limit (a timeout counts as a
  failure); when **50%** of the calls in the sliding window (4 calls, at least 2) fail, the circuit
  opens and the IGP is not called for **60 s**. While the IGP is unavailable the latest stored event is
  returned; with nothing stored the answer is 503 with `Retry-After: 60`.
- Default profile: `HttpIgpFeedOperation` calls `IGP_LATEST_EVENT_URL`, which must return the JSON of
  `IgpSeismicEventResponse`:

  ```json
  { "code": "IGP/CENSIS/RS 2026-0412", "magnitude": 5.8, "magnitudeScale": "Mw",
    "timestamp": "2026-10-06T15:29:41Z", "latitude": -12.05, "longitude": -77.12,
    "depthKm": 38.0, "reference": "Callao, 35 km SW" }
  ```

  Mapping the IGP's own public payload to this shape is pending.

## Correlation and risk index

1. Every `SensorReadingRecorded` is kept in memory for 30 minutes (`rumi.correlation.retention`).
2. A reading belongs to a seismic event when it was recorded between the event's `occurredAt` and
   `occurredAt + 5 min` (`rumi.correlation.window`). The correlation is attempted when a reading arrives
   and when a new IGP event is stored, and happens once per event and building.
3. The structural response of each zone is its peak vibration divided by
   `rumi.correlation.vibration-reference` (1.0), capped at 1. The building response is the highest one.
4. The strategy (`rumi.risk.strategy`: `rule-based` = `rule-based-v1`, or `ai` = `ai-placeholder-v1`)
   turns the response into a score, and the score into a level:

   | Score | Level |
   |---|---|
   | `[0.00, 0.35)` | LOW |
   | `[0.35, 0.65)` | MEDIUM |
   | `[0.65, 0.85)` | HIGH |
   | `[0.85, 1.00]` | CRITICAL |

   `RuleBasedRiskStrategy` returns 0.2, 0.5 or 0.8, so it gives LOW, MEDIUM or HIGH; CRITICAL needs a
   continuous strategy such as `AiRiskStrategy`.
5. The correlation is stored as `ANALYZED` with its risk index and model version. When the level is
   MEDIUM or higher, every zone whose own level is MEDIUM or higher becomes an affected zone, ranked
   from the highest response (rank 1).

## Events

| Event | Direction | Exchange | Routing key | Queue |
|---|---|---|---|---|
| `SensorReadingRecorded` | consumed | `rumi.structural-monitoring.events` (topic, durable) | `structural-monitoring.sensor-reading.recorded` | `rumi.seismic-correlation.sensor-reading-recorded` (durable) |

```json
{
  "eventId": "7c1f5b0e-3a52-4d0b-9f5e-2f6c1a8f4d11",
  "sensorId": "5d1c2f0e-8f4a-4a53-9a7e-0f3b1c9d7a11",
  "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
  "zone": "FLOOR-3-NORTH",
  "recordedAt": "2026-10-06T15:30:00Z",
  "value": 0.42
}
```

| Field | Type | Meaning |
|---|---|---|
| `eventId` | UUID | Id of the event |
| `sensorId` | UUID | Sensor that produced the reading |
| `buildingId` | UUID | Building of the sensor |
| `zone` | string, required | Zone of the reading, for example `FLOOR-3-NORTH` |
| `recordedAt` | ISO-8601 instant | When the reading was taken |
| `value` | number | Vibration of the reading |

A message without `zone` cannot be converted and is rejected without requeue.

The contract class is `com.rumi.seismiccorrelation.domain.event.SensorReadingRecorded`, this
service's own copy. The publisher (`rumi-monitoring-service`) keeps its own; a change must be
applied on both sides.

## API documentation

- Swagger UI: <http://localhost:8083/swagger-ui.html>
- OpenAPI spec: <http://localhost:8083/v3/api-docs>
- Exported spec: [`docs/openapi.json`](docs/openapi.json), exported from the dev profile

## Run

Requirements: JDK 21. Maven is not needed, the wrapper downloads it.

Without any infrastructure (H2 in memory, stub IGP feed, sample data, RabbitMQ listener off):

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# or
./mvnw package && java -jar target/rumi-seismic-service-0.1.0.jar --spring.profiles.active=dev
```

Default profile (PostgreSQL, RabbitMQ and the IGP feed through the variables below). Without RabbitMQ
the service starts and keeps retrying the connection:

```sh
./mvnw spring-boot:run
```

The schema is created and updated by Hibernate (`ddl-auto: update`); there is no migration tool yet.

| Variable | Default |
|---|---|
| `SERVER_PORT` | `8083` |
| `RABBITMQ_HOST` | `localhost` |
| `RABBITMQ_PORT` | `5672` |
| `RABBITMQ_USERNAME` | `guest` |
| `RABBITMQ_PASSWORD` | `guest` |
| `SEISMIC_DB_URL` | `jdbc:postgresql://localhost:5432/seismiceventsdb` |
| `SEISMIC_DB_USERNAME` | `postgres` |
| `SEISMIC_DB_PASSWORD` | `postgres` |
| `IGP_LATEST_EVENT_URL` | `http://localhost:8099/igp/latest-event` (placeholder) |

## Test

```sh
./mvnw test
```

The tests need neither a database nor a message broker nor the IGP: persistence tests run on H2.

## Structure

```
com.rumi.seismiccorrelation
├── application                  use cases: seismic events, correlation, risk index queries,
│                                SensorReadingRecordedHandler
├── domain
│   ├── event                    SensorReadingRecorded contract
│   ├── model                    SeismicEvent, CorrelatedSeismicEvent, RiskIndex, RiskLevel,
│   │                            AffectedZoneReport, EventStatus, RecentReading
│   ├── repository               repository ports
│   ├── service                  SeismicCorrelationService, RiskIndexCalculationService
│   └── strategy                 risk calculation strategies
└── infrastructure
    ├── config                   strategy selection and domain service beans
    ├── devdata                  sample data of the dev profile
    ├── external.igp             IGP feed client (circuit breaker), HTTP and stub feeds, mapper
    ├── memory                   in-memory store of recent readings
    ├── messaging.rabbitmq       subscriber and RabbitMQ configuration
    ├── persistence.jpa          entities and repositories of the four er_04 tables
    └── web                      REST controllers, DTOs, ProblemDetail handler, OpenAPI
```

## Known limitations

- The IGP is called on demand by `GET /api/v1/seismic-events/latest`; periodic polling is pending.
- The real IGP payload is not mapped yet (see above).
- Recent readings live in memory: they are lost on restart and not shared between instances.
- A correlation is calculated once, with the readings available at that moment; later readings of
  the same event do not update it. The `NOTIFIED` status belongs to the alerts flow, not implemented.
- `GET /api/v1/seismic-events` is not paginated.
- `AiRiskStrategy` is a deterministic placeholder until a trained model is integrated.
