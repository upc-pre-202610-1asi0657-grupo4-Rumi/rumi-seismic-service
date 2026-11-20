package com.rumi.seismiccorrelation.infrastructure.messaging.rabbitmq;

import com.rumi.seismiccorrelation.domain.event.CorrelatedSeismicEventCreated;
import com.rumi.seismiccorrelation.domain.event.CorrelatedSeismicEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitCorrelatedSeismicEventPublisher implements CorrelatedSeismicEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitCorrelatedSeismicEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitCorrelatedSeismicEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(CorrelatedSeismicEventCreated event) {
        LOGGER.info("Publishing CorrelatedSeismicEventCreated for building {} (riskLevel={})",
                event.buildingId(), event.riskLevel());
        rabbitTemplate.convertAndSend(
                SeismicCorrelationMessagingConfiguration.CORRELATED_EVENT_EXCHANGE,
                SeismicCorrelationMessagingConfiguration.CORRELATED_EVENT_ROUTING_KEY,
                event
        );
    }
}
