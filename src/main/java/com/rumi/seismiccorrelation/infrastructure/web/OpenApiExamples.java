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

    public static final String RISK_INDEX = """
            {
              "id": "c3a1e8d2-6b7f-4d09-9a21-5e8f7b6c4d3a",
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "correlatedEventId": "9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4",
              "level": "HIGH",
              "calculatedAt": "2026-10-06T15:32:10Z",
              "modelVersion": "rule-based-v1"
            }""";

    public static final String ERROR_BUILDING_WITHOUT_RISK_INDEX = """
            {
              "type": "about:blank",
              "title": "Not Found",
              "status": 404,
              "detail": "Building 0f1e2d3c-4b5a-4968-8776-5a4b3c2d1e0f has no risk index",
              "instance": "/api/v1/buildings/0f1e2d3c-4b5a-4968-8776-5a4b3c2d1e0f/risk-indexes/latest"
            }""";

    public static final String ERROR_MALFORMED_BUILDING_ID = """
            {
              "type": "about:blank",
              "title": "Bad Request",
              "status": 400,
              "detail": "buildingId must be a valid UUID",
              "instance": "/api/v1/buildings/building-7/risk-indexes/latest"
            }""";

    public static final String AFFECTED_ZONES = """
            [
              { "zone": "FLOOR-3-NORTH", "severityRank": 1 },
              { "zone": "FLOOR-2-NORTH", "severityRank": 2 }
            ]""";

    public static final String ERROR_UNKNOWN_RISK_INDEX = """
            {
              "type": "about:blank",
              "title": "Not Found",
              "status": 404,
              "detail": "Risk index 6d5c4b3a-2f1e-4d0c-9b8a-7f6e5d4c3b2a does not exist",
              "instance": "/api/v1/risk-indexes/6d5c4b3a-2f1e-4d0c-9b8a-7f6e5d4c3b2a/affected-zones"
            }""";

    public static final String ERROR_MALFORMED_RISK_INDEX_ID = """
            {
              "type": "about:blank",
              "title": "Bad Request",
              "status": 400,
              "detail": "riskIndexId must be a valid UUID",
              "instance": "/api/v1/risk-indexes/latest-risk/affected-zones"
            }""";

    private OpenApiExamples() {
    }
}
