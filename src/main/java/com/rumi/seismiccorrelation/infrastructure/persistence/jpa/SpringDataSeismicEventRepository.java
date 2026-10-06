package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataSeismicEventRepository extends JpaRepository<SeismicEventEntity, UUID> {

    Optional<SeismicEventEntity> findFirstByOccurredAtAndMagnitudeValueAndEpicenterLatitudeAndEpicenterLongitude(
            Instant occurredAt,
            double magnitudeValue,
            Double epicenterLatitude,
            Double epicenterLongitude
    );

    Optional<SeismicEventEntity> findFirstByOrderByOccurredAtDesc();
}
