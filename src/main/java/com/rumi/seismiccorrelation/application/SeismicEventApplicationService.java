package com.rumi.seismiccorrelation.application;

import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.SeismicEventRepository;
import com.rumi.seismiccorrelation.infrastructure.external.igp.IgpFeedClient;
import com.rumi.seismiccorrelation.infrastructure.external.igp.IgpFeedOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SeismicEventApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeismicEventApplicationService.class);

    private final IgpFeedClient igpFeedClient;
    private final IgpFeedOperation igpFeedOperation;
    private final SeismicEventRepository seismicEventRepository;
    private final SeismicCorrelationApplicationService correlationApplicationService;

    public SeismicEventApplicationService(
            IgpFeedClient igpFeedClient,
            IgpFeedOperation igpFeedOperation,
            SeismicEventRepository seismicEventRepository,
            SeismicCorrelationApplicationService correlationApplicationService
    ) {
        this.igpFeedClient = igpFeedClient;
        this.igpFeedOperation = igpFeedOperation;
        this.seismicEventRepository = seismicEventRepository;
        this.correlationApplicationService = correlationApplicationService;
    }

    /**
     * US13: latest event reported by the IGP, stored (and correlated) if it is new. When the IGP gives no event
     * (it failed or its circuit is open), the latest stored event is returned instead.
     */
    public SeismicEvent getLatestEvent() {
        Optional<SeismicEvent> fromIgp = igpFeedClient.fetchLatestEvent(igpFeedOperation);
        if (fromIgp.isPresent()) {
            return storeIfNew(fromIgp.get());
        }
        LOGGER.warn("No event from the IGP feed, falling back to the latest stored event");
        return seismicEventRepository.findLatest().orElseThrow(SeismicFeedUnavailableException::new);
    }

    /** US13: stored events, newest first, optionally between from and to (both inclusive). */
    public List<SeismicEvent> listEvents(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidQueryException("from must not be after to");
        }
        return seismicEventRepository.findOccurredBetween(from, to);
    }

    private SeismicEvent storeIfNew(SeismicEvent event) {
        Optional<SeismicEvent> stored = seismicEventRepository.findSameOccurrence(event);
        if (stored.isPresent()) {
            return stored.get();
        }
        SeismicEvent saved;
        try {
            saved = seismicEventRepository.save(event);
        } catch (DataIntegrityViolationException storedConcurrently) {
            return seismicEventRepository.findSameOccurrence(event).orElseThrow(() -> storedConcurrently);
        }
        correlationApplicationService.processOfficialEvent(saved);
        return saved;
    }
}
