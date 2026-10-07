package com.rumi.seismiccorrelation.infrastructure.external.igp;

import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class IgpFeedClient {

    static final String IGP_FEED = "igpFeed";

    private final IgpSeismicEventMapper mapper;
    private final TimeLimiter timeLimiter;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public IgpFeedClient(IgpSeismicEventMapper mapper, TimeLimiterRegistry timeLimiterRegistry) {
        this.mapper = mapper;
        this.timeLimiter = timeLimiterRegistry.timeLimiter(IGP_FEED);
    }

    public SeismicEvent processResponse(IgpSeismicEventResponse response) {
        return mapper.toDomain(response);
    }

    /**
     * Calls the IGP within the time limit of igpFeed (a timeout counts as a failure of the
     * circuit). When the call fails, times out or the circuit is open, no event is returned.
     */
    @CircuitBreaker(name = IGP_FEED, fallbackMethod = "igpUnavailableFallback")
    public Optional<SeismicEvent> fetchLatestEvent(IgpFeedOperation feedOperation) {
        return Optional.ofNullable(callWithinTimeLimit(feedOperation))
                .map(this::processResponse);
    }

    public Optional<SeismicEvent> igpUnavailableFallback(
            IgpFeedOperation feedOperation,
            Throwable failure
    ) {
        return Optional.empty();
    }

    private IgpSeismicEventResponse callWithinTimeLimit(IgpFeedOperation feedOperation) {
        try {
            return timeLimiter.executeFutureSupplier(() -> executor.submit(feedOperation::fetchLatest));
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IgpFeedException(failure);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    static class IgpFeedException extends RuntimeException {

        IgpFeedException(Throwable cause) {
            super("IGP feed call failed: " + cause, cause);
        }
    }
}
