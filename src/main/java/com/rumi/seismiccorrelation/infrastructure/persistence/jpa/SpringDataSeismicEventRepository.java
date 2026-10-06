package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataSeismicEventRepository
        extends JpaRepository<SeismicEventEntity, UUID>, JpaSpecificationExecutor<SeismicEventEntity> {

    Optional<SeismicEventEntity> findFirstByOccurredAtAndMagnitudeValueAndEpicenterLatitudeAndEpicenterLongitude(
            Instant occurredAt,
            double magnitudeValue,
            Double epicenterLatitude,
            Double epicenterLongitude
    );

    Optional<SeismicEventEntity> findFirstByOrderByOccurredAtDesc();
}
