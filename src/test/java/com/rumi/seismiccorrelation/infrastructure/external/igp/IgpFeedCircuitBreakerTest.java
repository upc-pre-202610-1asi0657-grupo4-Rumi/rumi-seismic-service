package com.rumi.seismiccorrelation.infrastructure.external.igp;

import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = IgpFeedCircuitBreakerTest.TestApplication.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
        }
)
class IgpFeedCircuitBreakerTest {

    @Autowired
    private IgpFeedClient client;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private TimeLimiterRegistry timeLimiterRegistry;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void resetCircuit() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker(IgpFeedClient.IGP_FEED);
        circuitBreaker.reset();
    }

    @Test
    void usesTheDesignParameters() {
        CircuitBreakerConfig config = circuitBreaker.getCircuitBreakerConfig();

        assertThat(config.getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(60).toMillis());
        assertThat(timeLimiterRegistry.timeLimiter(IgpFeedClient.IGP_FEED).getTimeLimiterConfig().getTimeoutDuration())
                .isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void returnsTheMappedEventWhenTheIgpOperationSucceeds() {
        IgpSeismicEventResponse response = new IgpSeismicEventResponse(
                "IGP-2026-002",
                4.8,
                Instant.parse("2026-10-06T16:00:00Z"),
                -11.9,
                -77.1,
                42.0
        );

        Optional<SeismicEvent> result = client.fetchLatestEvent(() -> response);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo("IGP-2026-002");
    }

    @Test
    void returnsNoEventWhenTheIgpOperationFails() {
        Optional<SeismicEvent> result = client.fetchLatestEvent(() -> {
            throw new IllegalStateException("IGP service unavailable");
        });

        assertThat(result).isEmpty();
    }

    @Test
    void opensTheCircuitWhenHalfOfTheCallsFailAndStopsCallingTheIgp() {
        AtomicInteger calls = new AtomicInteger();
        IgpFeedOperation failingFeed = () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("IGP service unavailable");
        };

        client.fetchLatestEvent(failingFeed);
        client.fetchLatestEvent(failingFeed);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(client.fetchLatestEvent(failingFeed)).isEmpty();
        assertThat(calls).hasValue(2);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        IgpSeismicEventMapper igpSeismicEventMapper() {
            return new IgpSeismicEventMapper();
        }

        @Bean
        IgpFeedClient igpFeedClient(IgpSeismicEventMapper mapper, TimeLimiterRegistry timeLimiterRegistry) {
            return new IgpFeedClient(mapper, timeLimiterRegistry);
        }
    }
}
