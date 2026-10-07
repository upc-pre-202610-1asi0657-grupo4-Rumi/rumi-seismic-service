package com.rumi.seismiccorrelation.infrastructure.web.dto;

import com.rumi.seismiccorrelation.domain.model.AffectedZoneReport;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Zone of the building affected by the seismic event")
public record AffectedZoneResponse(
        @Schema(description = "Zone of the building", example = "FLOOR-3-NORTH")
        String zone,
        @Schema(description = "Severity rank, 1 is the most affected zone", example = "1")
        int severityRank
) {
    public static AffectedZoneResponse fromDomain(AffectedZoneReport report) {
        return new AffectedZoneResponse(report.zone(), report.severityRank());
    }
}
