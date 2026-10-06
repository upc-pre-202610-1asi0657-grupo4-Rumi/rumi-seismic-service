package com.rumi.seismiccorrelation.infrastructure.web;

import com.rumi.seismiccorrelation.application.SeismicEventApplicationService;
import com.rumi.seismiccorrelation.infrastructure.web.dto.SeismicEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/seismic-events", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Seismic events", description = "Earthquakes reported by the IGP (US13)")
public class SeismicEventsController {

    private final SeismicEventApplicationService applicationService;

    public SeismicEventsController(SeismicEventApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/latest")
    @Operation(
            summary = "Get the latest seismic event reported by the IGP",
            description = "US13. Asks the IGP feed for its latest event through a circuit breaker and stores "
                    + "it if it is new (same occurrence time, magnitude and epicenter means the same event). "
                    + "If the IGP fails, times out or its circuit is open, the latest stored event is "
                    + "returned instead; 503 only when there is no stored event either."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Latest event, from the IGP or, if it is unavailable, the latest stored one",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SeismicEventResponse.class),
                    examples = @ExampleObject(
                            name = "latestEvent",
                            summary = "Earthquake off Callao",
                            value = OpenApiExamples.SEISMIC_EVENT
                    )
            )
    )
    @ApiResponse(
            responseCode = "503",
            description = "The IGP is unavailable and there is no stored event to fall back to",
            headers = @Header(name = "Retry-After", description = "Seconds to wait before retrying",
                    schema = @Schema(type = "integer", example = "60")),
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(
                            name = "igpUnavailable",
                            summary = "IGP down and no stored event",
                            value = OpenApiExamples.ERROR_IGP_UNAVAILABLE
                    )
            )
    )
    public SeismicEventResponse getLatestEvent() {
        return SeismicEventResponse.fromDomain(applicationService.getLatestEvent());
    }

    @GetMapping
    @Operation(
            summary = "List the stored seismic events",
            description = "US13. Returns the events stored by Rumi, newest first. from and to are optional "
                    + "ISO-8601 instants, both inclusive; without them every stored event is returned. "
                    + "This endpoint reads the database only and does not call the IGP."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Stored events, newest first (empty list if there are none in the range)",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = SeismicEventResponse.class)),
                    examples = @ExampleObject(
                            name = "seismicEvents",
                            summary = "Two stored events",
                            value = OpenApiExamples.SEISMIC_EVENTS
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "from or to is not an ISO-8601 instant, or from is after to",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                            @ExampleObject(
                                    name = "malformedFrom",
                                    summary = "from is not an ISO-8601 instant",
                                    value = OpenApiExamples.ERROR_MALFORMED_FROM
                            ),
                            @ExampleObject(
                                    name = "reversedRange",
                                    summary = "from is after to",
                                    value = OpenApiExamples.ERROR_REVERSED_RANGE
                            )
                    }
            )
    )
    public List<SeismicEventResponse> listEvents(
            @Parameter(description = "Earliest occurrence time, inclusive (ISO-8601 instant, optional)",
                    example = "2026-10-01T00:00:00Z")
            @RequestParam(required = false) Instant from,
            @Parameter(description = "Latest occurrence time, inclusive (ISO-8601 instant, optional)",
                    example = "2026-10-07T00:00:00Z")
            @RequestParam(required = false) Instant to
    ) {
        return applicationService.listEvents(from, to).stream()
                .map(SeismicEventResponse::fromDomain)
                .toList();
    }
}
