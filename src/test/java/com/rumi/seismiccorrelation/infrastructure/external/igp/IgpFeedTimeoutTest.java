package com.rumi.seismiccorrelation.infrastructure.external.igp;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Same wiring as production with a short time limit, so the test does not wait 5 s. */
@SpringBootTest(
        classes = IgpFeedTimeoutTest.TestApplication.class,
        properties = {
                "resilience4j.timelimiter.instances.igpFeed.timeout-duration=200ms",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
        }
)
class IgpFeedTimeoutTest {

    @Autowired
    private IgpFeedClient client;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void aSlowIgpCallTimesOutAndCountsAsAFailure() {
        long start = System.nanoTime();

        var result = client.fetchLatestEvent(() -> {
            try {
                Thread.sleep(Duration.ofSeconds(3));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return new IgpSeismicEventResponse("IGP-late", 5.0, Instant.parse("2026-10-06T16:00:00Z"), -12, -77, 30);
        });

        assertThat(result).isEmpty();
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
        assertThat(circuitBreakerRegistry.circuitBreaker(IgpFeedClient.IGP_FEED).getMetrics().getNumberOfFailedCalls())
                .isEqualTo(1);
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
