package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.SeismicEventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
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

    @Override
    public Optional<SeismicEvent> findLatest() {
        return springDataRepository.findFirstByOrderByOccurredAtDesc().map(SeismicEventEntity::toDomain);
    }

    @Override
    public List<SeismicEvent> findOccurredBetween(Instant from, Instant to) {
        Specification<SeismicEventEntity> occurredBetween = (root, query, builder) -> builder.and(
                from == null ? builder.conjunction() : builder.greaterThanOrEqualTo(root.get("occurredAt"), from),
                to == null ? builder.conjunction() : builder.lessThanOrEqualTo(root.get("occurredAt"), to)
        );
        return springDataRepository.findAll(occurredBetween, Sort.by(Sort.Direction.DESC, "occurredAt")).stream()
                .map(SeismicEventEntity::toDomain)
                .toList();
    }

    private static UUID storedIdOf(SeismicEvent event) {
        try {
            return UUID.fromString(event.getId());
        } catch (IllegalArgumentException igpCode) {
            return UUID.randomUUID();
        }
    }
}
