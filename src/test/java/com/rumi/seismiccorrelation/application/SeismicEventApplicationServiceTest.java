package com.rumi.seismiccorrelation.application;

import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.SeismicEventRepository;
import com.rumi.seismiccorrelation.infrastructure.external.igp.IgpFeedClient;
import com.rumi.seismiccorrelation.infrastructure.external.igp.IgpFeedOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeismicEventApplicationServiceTest {

    static final SeismicEvent FROM_IGP = new SeismicEvent(
            "IGP-2026-0412", 5.8, "Mw", Instant.parse("2026-10-06T15:29:41Z"), -12.05, -77.12, 38.0,
            "Callao, 35 km SW");
    static final SeismicEvent STORED = new SeismicEvent(
            "1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9", 5.8, "Mw", Instant.parse("2026-10-06T15:29:41Z"),
            -12.05, -77.12, Double.NaN, "Callao, 35 km SW");

    private IgpFeedClient igpFeedClient;
    private IgpFeedOperation igpFeedOperation;
    private SeismicEventRepository repository;
    private SeismicEventApplicationService service;

    @BeforeEach
    void setUp() {
        igpFeedClient = mock(IgpFeedClient.class);
        igpFeedOperation = mock(IgpFeedOperation.class);
        repository = mock(SeismicEventRepository.class);
        service = new SeismicEventApplicationService(igpFeedClient, igpFeedOperation, repository);
    }

    @Test
    void storesANewEventReportedByTheIgp() {
        when(igpFeedClient.fetchLatestEvent(igpFeedOperation)).thenReturn(Optional.of(FROM_IGP));
        when(repository.findSameOccurrence(FROM_IGP)).thenReturn(Optional.empty());
        when(repository.save(FROM_IGP)).thenReturn(STORED);

        assertThat(service.getLatestEvent()).isSameAs(STORED);
    }

    @Test
    void doesNotStoreAnEventThatIsAlreadyStored() {
        when(igpFeedClient.fetchLatestEvent(igpFeedOperation)).thenReturn(Optional.of(FROM_IGP));
        when(repository.findSameOccurrence(FROM_IGP)).thenReturn(Optional.of(STORED));

        assertThat(service.getLatestEvent()).isSameAs(STORED);
        verify(repository, never()).save(any());
    }

    @Test
    void returnsTheEventStoredConcurrentlyByAnotherRequest() {
        when(igpFeedClient.fetchLatestEvent(igpFeedOperation)).thenReturn(Optional.of(FROM_IGP));
        when(repository.findSameOccurrence(FROM_IGP)).thenReturn(Optional.empty(), Optional.of(STORED));
        when(repository.save(FROM_IGP)).thenThrow(new DataIntegrityViolationException("uk_seismic_events_occurrence"));

        assertThat(service.getLatestEvent()).isSameAs(STORED);
    }

    @Test
    void fallsBackToTheLatestStoredEventWhenTheIgpGivesNoEvent() {
        when(igpFeedClient.fetchLatestEvent(igpFeedOperation)).thenReturn(Optional.empty());
        when(repository.findLatest()).thenReturn(Optional.of(STORED));

        assertThat(service.getLatestEvent()).isSameAs(STORED);
    }

    @Test
    void failsWhenTheIgpGivesNoEventAndNothingIsStored() {
        when(igpFeedClient.fetchLatestEvent(igpFeedOperation)).thenReturn(Optional.empty());
        when(repository.findLatest()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatestEvent()).isInstanceOf(SeismicFeedUnavailableException.class);
    }

    @Test
    void listsTheStoredEventsInTheRange() {
        Instant from = Instant.parse("2026-10-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-07T00:00:00Z");
        when(repository.findOccurredBetween(from, to)).thenReturn(List.of(STORED));

        assertThat(service.listEvents(from, to)).containsExactly(STORED);
    }

    @Test
    void listsEveryStoredEventWithoutBounds() {
        when(repository.findOccurredBetween(null, null)).thenReturn(List.of(STORED));

        assertThat(service.listEvents(null, null)).containsExactly(STORED);
    }

    @Test
    void rejectsAReversedRange() {
        assertThatThrownBy(() -> service.listEvents(
                Instant.parse("2026-10-07T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z")))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessage("from must not be after to");
    }
}
