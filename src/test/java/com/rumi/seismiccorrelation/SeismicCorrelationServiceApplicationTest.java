package com.rumi.seismiccorrelation;

import com.rumi.seismiccorrelation.infrastructure.external.igp.IgpFeedClient;
import com.rumi.seismiccorrelation.infrastructure.messaging.rabbitmq.SensorReadingRecordedSubscriber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureTestDatabase
class SeismicCorrelationServiceApplicationTest {

    @Autowired
    private SensorReadingRecordedSubscriber subscriber;

    @Autowired
    private IgpFeedClient igpFeedClient;

    @Test
    void contextLoadsWithoutABroker() {
        assertThat(subscriber).isNotNull();
        assertThat(igpFeedClient).isNotNull();
    }
}
