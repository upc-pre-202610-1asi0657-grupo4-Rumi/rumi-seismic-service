package com.rumi.seismiccorrelation.application;

import com.rumi.seismiccorrelation.domain.event.SensorReadingRecorded;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SensorReadingRecordedHandlerTest {

    @Test
    void passesTheReadingToTheCorrelation() {
        SeismicCorrelationApplicationService correlationService = mock(SeismicCorrelationApplicationService.class);
        SensorReadingRecorded event = new SensorReadingRecorded(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "FLOOR-3-NORTH",
                Instant.parse("2026-10-06T15:30:00Z"), 0.42);

        new SensorReadingRecordedHandler(correlationService).handle(event);

        verify(correlationService).recordReading(event);
    }
}
