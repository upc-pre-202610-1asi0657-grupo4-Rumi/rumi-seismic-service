package com.rumi.seismiccorrelation.infrastructure.web;

import com.rumi.seismiccorrelation.application.RiskIndexQueryService;
import com.rumi.seismiccorrelation.infrastructure.web.dto.RiskIndexResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Risk indexes", description = "Risk of a building after a seismic event and its affected zones (US11, US12)")
public class RiskIndexesController {

    private final RiskIndexQueryService queryService;

    public RiskIndexesController(RiskIndexQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/buildings/{buildingId}/risk-indexes/latest")
    @Operation(
            summary = "Get the latest risk index of a building",
            description = "US11. Returns the most recently calculated risk index of the building, with the "
                    + "correlation it comes from and the model that calculated it. A risk index is created "
                    + "when a seismic event reported by the IGP matches the sensor readings of the building."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Latest risk index of the building",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RiskIndexResponse.class),
                    examples = @ExampleObject(
                            name = "latestRiskIndex",
                            summary = "HIGH risk after the Callao earthquake",
                            value = OpenApiExamples.RISK_INDEX
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "buildingId is not a UUID",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(
                            name = "malformedBuildingId",
                            summary = "buildingId is not a UUID",
                            value = OpenApiExamples.ERROR_MALFORMED_BUILDING_ID
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The building has no risk index",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(
                            name = "noRiskIndex",
                            summary = "No seismic event has been correlated with the building",
                            value = OpenApiExamples.ERROR_BUILDING_WITHOUT_RISK_INDEX
                    )
            )
    )
    public RiskIndexResponse getLatestRiskIndex(
            @Parameter(description = "Building whose risk index is queried", required = true,
                    example = OpenApiExamples.BUILDING_ID)
            @PathVariable UUID buildingId
    ) {
        return RiskIndexResponse.fromDomain(queryService.getLatestRiskIndex(buildingId));
    }
}
