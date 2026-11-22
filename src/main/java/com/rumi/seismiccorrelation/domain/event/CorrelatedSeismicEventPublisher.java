package com.rumi.seismiccorrelation.domain.event;

/**
 * Publishes correlated seismic risk events to downstream bounded contexts (such as Notification).
 */
@FunctionalInterface
public interface CorrelatedSeismicEventPublisher {

    void publish(CorrelatedSeismicEventCreated event);
}
