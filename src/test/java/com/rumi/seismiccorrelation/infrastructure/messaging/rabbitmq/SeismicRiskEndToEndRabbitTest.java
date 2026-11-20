package com.rumi.seismiccorrelation.infrastructure.messaging.rabbitmq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumi.seismiccorrelation.domain.event.SensorReadingRecorded;
import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.SeismicEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end integration test over a real RabbitMQ broker:
 * 1. Structural Monitoring publishes SensorReadingRecorded to rumi.structural-monitoring.events
 * 2. Seismic Correlation consumes the reading, correlates it with the stored IGP earthquake,
 *    calculates the building risk index and publishes CorrelatedSeismicEventCreated to
 *    rumi.seismic-correlation.events
 * 3. Notification queue (rumi.notification.correlated-seismic-event-created) receives the alert payload.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SeismicRiskEndToEndRabbitTest {

    private static final String NOTIFICATION_QUEUE = "rumi.notification.correlated-seismic-event-created";

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

    @DynamicPropertySource
    static void registerRabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:rumi_seismic_e2e;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private SeismicEventRepository seismicEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void endToEndFlowFromSensorReadingThroughRiskCalculationToNotificationAlertQueue() throws Exception {
        // 1. Declare Notification's consumer queue bound to the seismic correlation exchange
        TopicExchange riskExchange = new TopicExchange(
                SeismicCorrelationMessagingConfiguration.CORRELATED_EVENT_EXCHANGE, true, false);
        Queue alertQueue = QueueBuilder.durable(NOTIFICATION_QUEUE).build();
        amqpAdmin.declareExchange(riskExchange);
        amqpAdmin.declareQueue(alertQueue);
        amqpAdmin.declareBinding(BindingBuilder.bind(alertQueue)
                .to(riskExchange)
                .with(SeismicCorrelationMessagingConfiguration.CORRELATED_EVENT_ROUTING_KEY));

        // 2. Store an official IGP seismic event
        Instant earthquakeTime = Instant.now().minusSeconds(15);
        SeismicEvent savedEarthquake = seismicEventRepository.save(new SeismicEvent(
                "IGP-2026-E2E", 6.1, earthquakeTime, -12.05, -77.12, 35.0));
        UUID seismicEventId = UUID.fromString(savedEarthquake.getId());
        UUID buildingId = UUID.randomUUID();
        UUID sensorId = UUID.randomUUID();

        // 3. Simulate Monitoring publishing SensorReadingRecorded to RabbitMQ
        SensorReadingRecorded sensorReading = new SensorReadingRecorded(
                UUID.randomUUID(),
                sensorId,
                buildingId,
                "FLOOR-3-NORTH",
                earthquakeTime.plusSeconds(5),
                0.85
        );
        rabbitTemplate.convertAndSend(
                SeismicCorrelationMessagingConfiguration.SENSOR_READING_EXCHANGE,
                SeismicCorrelationMessagingConfiguration.SENSOR_READING_ROUTING_KEY,
                sensorReading
        );

        // 4. Verify the correlated risk event reaches Notification's alert queue on real RabbitMQ
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Message message = rabbitTemplate.receive(NOTIFICATION_QUEUE, 1_000);
            assertThat(message).isNotNull();

            JsonNode alertPayload = objectMapper.readTree(message.getBody());
            assertThat(UUID.fromString(alertPayload.get("buildingId").asText())).isEqualTo(buildingId);
            assertThat(UUID.fromString(alertPayload.get("seismicEventId").asText())).isEqualTo(seismicEventId);
            assertThat(UUID.fromString(alertPayload.get("riskIndexId").asText())).isNotNull();
            assertThat(alertPayload.get("riskLevel").asText()).isEqualTo("HIGH");
            assertThat(Instant.parse(alertPayload.get("calculatedAt").asText())).isNotNull();
        });
    }
}
