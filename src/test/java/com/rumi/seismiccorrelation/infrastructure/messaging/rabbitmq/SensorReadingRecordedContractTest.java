package com.rumi.seismiccorrelation.infrastructure.messaging.rabbitmq;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.rumi.seismiccorrelation.domain.event.SensorReadingRecorded;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SensorReadingRecordedContractTest {

    private static final String PAYLOAD = """
            {
              "eventId": "7c1f5b0e-3a52-4d0b-9f5e-2f6c1a8f4d11",
              "sensorId": "5d1c2f0e-8f4a-4a53-9a7e-0f3b1c9d7a11",
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "zone": "FLOOR-3-NORTH",
              "recordedAt": "2026-10-06T15:30:00Z",
              "value": 0.42
            }""";

    @Test
    void readsTheEventPublishedByTheMonitoringServiceIntoItsOwnContractClass() {
        MessageConverter converter = new SeismicCorrelationMessagingConfiguration()
                .seismicCorrelationMessageConverter(JsonMapper.builder().findAndAddModules().build());
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        // Class name written by the publisher; it does not exist in this service.
        properties.setHeader("__TypeId__", "com.rumi.structuralmonitoring.domain.event.SensorReadingRecorded");
        // What the listener container infers from the @RabbitListener method parameter.
        properties.setInferredArgumentType(SensorReadingRecorded.class);

        Object event = converter.fromMessage(new Message(PAYLOAD.getBytes(StandardCharsets.UTF_8), properties));

        assertThat(event).isEqualTo(new SensorReadingRecorded(
                UUID.fromString("7c1f5b0e-3a52-4d0b-9f5e-2f6c1a8f4d11"),
                UUID.fromString("5d1c2f0e-8f4a-4a53-9a7e-0f3b1c9d7a11"),
                UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d"),
                "FLOOR-3-NORTH",
                Instant.parse("2026-10-06T15:30:00Z"),
                0.42
        ));
    }

    @Test
    void rejectsAnEventWithoutZone() {
        MessageConverter converter = new SeismicCorrelationMessagingConfiguration()
                .seismicCorrelationMessageConverter(JsonMapper.builder().findAndAddModules().build());
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setInferredArgumentType(SensorReadingRecorded.class);
        String payloadWithoutZone = PAYLOAD.replace("\"zone\": \"FLOOR-3-NORTH\",", "");

        assertThatThrownBy(() -> converter.fromMessage(
                new Message(payloadWithoutZone.getBytes(StandardCharsets.UTF_8), properties)))
                .isInstanceOf(MessageConversionException.class);
    }
}
