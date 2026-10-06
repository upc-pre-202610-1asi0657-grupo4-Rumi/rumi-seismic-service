package com.rumi.seismiccorrelation.domain.repository;

import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;

import java.util.Optional;
import java.util.UUID;

/** Stores the correlation aggregate together with its risk index and affected zones. */
public interface CorrelatedSeismicEventRepository {

    CorrelatedSeismicEvent save(CorrelatedSeismicEvent correlatedEvent);

    Optional<CorrelatedSeismicEvent> findById(UUID id);
}
