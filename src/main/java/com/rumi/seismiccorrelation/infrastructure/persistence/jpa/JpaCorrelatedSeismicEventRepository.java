package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.CorrelatedSeismicEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCorrelatedSeismicEventRepository implements CorrelatedSeismicEventRepository {

    private final SpringDataCorrelatedSeismicEventRepository springDataRepository;

    public JpaCorrelatedSeismicEventRepository(SpringDataCorrelatedSeismicEventRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    @Transactional
    public CorrelatedSeismicEvent save(CorrelatedSeismicEvent correlatedEvent) {
        return springDataRepository.save(CorrelatedSeismicEventEntity.fromDomain(correlatedEvent)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CorrelatedSeismicEvent> findById(UUID id) {
        return springDataRepository.findById(id).map(CorrelatedSeismicEventEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySeismicEventIdAndBuildingId(UUID seismicEventId, UUID buildingId) {
        return springDataRepository.existsBySeismicEventIdAndBuildingId(seismicEventId, buildingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CorrelatedSeismicEvent> findLatestWithRiskIndexByBuildingId(UUID buildingId) {
        return springDataRepository.findWithRiskIndexByBuildingIdNewestFirst(buildingId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(CorrelatedSeismicEventEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CorrelatedSeismicEvent> findByRiskIndexId(UUID riskIndexId) {
        return springDataRepository.findByRiskIndexId(riskIndexId).map(CorrelatedSeismicEventEntity::toDomain);
    }
}
