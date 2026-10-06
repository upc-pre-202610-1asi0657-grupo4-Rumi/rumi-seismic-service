package com.rumi.seismiccorrelation.infrastructure.messaging.rabbitmq;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.rumi.seismiccorrelation.domain.event.SensorReadingRecorded;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SensorReadingRecordedContractTest {

    private static final String PAYLOAD = """
            {
              "eventId": "7c1f5b0e-3a52-4d0b-9f5e-2f6c1a8f4d11",
              "sensorId": "b2a9d0f4-6c1e-4a57-8f0a-91d2c3e4f5a6",
              "buildingId": "3f2c8a10-5d7b-4e9a-b1c2-0a1b2c3d4e5f",
              "recordedAt": "2026-10-06T15:30:00Z",
              "value": 0.018
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
                UUID.fromString("b2a9d0f4-6c1e-4a57-8f0a-91d2c3e4f5a6"),
                UUID.fromString("3f2c8a10-5d7b-4e9a-b1c2-0a1b2c3d4e5f"),
                Instant.parse("2026-10-06T15:30:00Z"),
                0.018
        ));
    }
}
