package com.rumi.seismiccorrelation.domain.repository;

import com.rumi.seismiccorrelation.domain.model.SeismicEvent;

import java.util.Optional;
import java.util.UUID;

public interface SeismicEventRepository {

    /**
     * Stores the event and returns it with its stored id. An id that is a UUID is kept;
     * any other id (an IGP code) is replaced by a new UUID.
     */
    SeismicEvent save(SeismicEvent event);

    /** A stored event with the same occurrence time, magnitude and epicenter. */
    Optional<SeismicEvent> findSameOccurrence(SeismicEvent event);

    Optional<SeismicEvent> findById(UUID id);

    /** The stored event that occurred last. */
    Optional<SeismicEvent> findLatest();
}
