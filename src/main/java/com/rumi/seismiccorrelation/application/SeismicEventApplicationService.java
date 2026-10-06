package com.rumi.seismiccorrelation.application;

import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.SeismicEventRepository;
import com.rumi.seismiccorrelation.infrastructure.external.igp.IgpFeedClient;
import com.rumi.seismiccorrelation.infrastructure.external.igp.IgpFeedOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SeismicEventApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeismicEventApplicationService.class);

    private final IgpFeedClient igpFeedClient;
    private final IgpFeedOperation igpFeedOperation;
    private final SeismicEventRepository seismicEventRepository;

    public SeismicEventApplicationService(
            IgpFeedClient igpFeedClient,
            IgpFeedOperation igpFeedOperation,
            SeismicEventRepository seismicEventRepository
    ) {
        this.igpFeedClient = igpFeedClient;
        this.igpFeedOperation = igpFeedOperation;
        this.seismicEventRepository = seismicEventRepository;
    }

    /**
     * US13: latest event reported by the IGP, stored if it is new. When the IGP gives no event
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

    private SeismicEvent storeIfNew(SeismicEvent event) {
        Optional<SeismicEvent> stored = seismicEventRepository.findSameOccurrence(event);
        if (stored.isPresent()) {
            return stored.get();
        }
        try {
            return seismicEventRepository.save(event);
        } catch (DataIntegrityViolationException storedConcurrently) {
            return seismicEventRepository.findSameOccurrence(event).orElseThrow(() -> storedConcurrently);
        }
    }
}
