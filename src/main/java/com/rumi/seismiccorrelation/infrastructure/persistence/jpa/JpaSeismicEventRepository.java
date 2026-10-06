package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.SeismicEventRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaSeismicEventRepository implements SeismicEventRepository {

    private final SpringDataSeismicEventRepository springDataRepository;

    public JpaSeismicEventRepository(SpringDataSeismicEventRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public SeismicEvent save(SeismicEvent event) {
        return springDataRepository.save(SeismicEventEntity.fromDomain(storedIdOf(event), event)).toDomain();
    }

    @Override
    public Optional<SeismicEvent> findSameOccurrence(SeismicEvent event) {
        return springDataRepository
                .findFirstByOccurredAtAndMagnitudeValueAndEpicenterLatitudeAndEpicenterLongitude(
                        event.getOccurredAt(), event.getMagnitude(), event.getLatitude(), event.getLongitude())
                .map(SeismicEventEntity::toDomain);
    }

    @Override
    public Optional<SeismicEvent> findById(UUID id) {
        return springDataRepository.findById(id).map(SeismicEventEntity::toDomain);
    }

    private static UUID storedIdOf(SeismicEvent event) {
        try {
            return UUID.fromString(event.getId());
        } catch (IllegalArgumentException igpCode) {
            return UUID.randomUUID();
        }
    }
}
