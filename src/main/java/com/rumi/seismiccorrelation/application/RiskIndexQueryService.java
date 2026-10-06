package com.rumi.seismiccorrelation.application;

import com.rumi.seismiccorrelation.domain.model.AffectedZoneReport;
import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.RiskIndex;
import com.rumi.seismiccorrelation.domain.repository.CorrelatedSeismicEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RiskIndexQueryService {

    private final CorrelatedSeismicEventRepository correlatedEventRepository;

    public RiskIndexQueryService(CorrelatedSeismicEventRepository correlatedEventRepository) {
        this.correlatedEventRepository = correlatedEventRepository;
    }

    /** US11: the correlation of the building with the most recent risk index. */
    public CorrelatedSeismicEvent getLatestRiskIndex(UUID buildingId) {
        return correlatedEventRepository.findLatestWithRiskIndexByBuildingId(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building " + buildingId + " has no risk index"));
    }

    /** US12: affected zones of the risk index by severity rank; empty when the level is below MEDIUM. */
    public List<AffectedZoneReport> getAffectedZones(UUID riskIndexId) {
        return correlatedEventRepository.findByRiskIndexId(riskIndexId)
                .flatMap(CorrelatedSeismicEvent::getRiskIndex)
                .map(RiskIndex::getAffectedZones)
                .orElseThrow(() -> new ResourceNotFoundException("Risk index " + riskIndexId + " does not exist"));
    }
}
