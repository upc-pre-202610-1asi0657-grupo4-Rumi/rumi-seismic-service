package com.rumi.seismiccorrelation.infrastructure.web;

import com.rumi.seismiccorrelation.application.SeismicEventApplicationService;
import com.rumi.seismiccorrelation.infrastructure.web.dto.SeismicEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
