package com.rumi.seismiccorrelation.infrastructure.web;

/**
 * Example payloads shown in the OpenAPI documentation. The ids are those of the sample data
 * loaded by the dev profile.
 */
public final class OpenApiExamples {

    public static final String BUILDING_ID = "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d";
    public static final String RISK_INDEX_ID = "c3a1e8d2-6b7f-4d09-9a21-5e8f7b6c4d3a";
    public static final String PROBLEM_JSON = "application/problem+json";

    public static final String SEISMIC_EVENT = """
            {
              "id": "1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9",
              "occurredAt": "2026-10-06T15:29:41Z",
              "magnitude": { "value": 5.8, "scale": "Mw" },
              "epicenter": { "latitude": -12.05, "longitude": -77.12, "description": "Callao, 35 km SW" },
              "source": "IGP"
            }""";

    public static final String ERROR_IGP_UNAVAILABLE = """
            {
              "type": "about:blank",
              "title": "Service Unavailable",
              "status": 503,
              "detail": "The IGP feed is unavailable and no seismic event is stored yet",
              "instance": "/api/v1/seismic-events/latest"
            }""";

    public static final String SEISMIC_EVENTS = """
            [
              {
                "id": "1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9",
                "occurredAt": "2026-10-06T15:29:41Z",
                "magnitude": { "value": 5.8, "scale": "Mw" },
                "epicenter": { "latitude": -12.05, "longitude": -77.12, "description": "Callao, 35 km SW" },
                "source": "IGP"
              },
              {
                "id": "2c3d4e5f-6071-4829-93a4-b5c6d7e8f9a0",
                "occurredAt": "2026-10-03T22:10:05Z",
                "magnitude": { "value": 4.2, "scale": "ML" },
                "epicenter": { "latitude": -14.07, "longitude": -75.73, "description": "Ica, 20 km W" },
                "source": "IGP"
              }
            ]""";

    public static final String ERROR_MALFORMED_FROM = """
            {
              "type": "about:blank",
              "title": "Bad Request",
              "status": 400,
              "detail": "from must be an ISO-8601 instant, for example 2026-10-06T15:30:00Z",
              "instance": "/api/v1/seismic-events"
            }""";

    public static final String ERROR_REVERSED_RANGE = """
            {
              "type": "about:blank",
              "title": "Bad Request",
              "status": 400,
              "detail": "from must not be after to",
              "instance": "/api/v1/seismic-events"
            }""";

    private OpenApiExamples() {
    }
}
