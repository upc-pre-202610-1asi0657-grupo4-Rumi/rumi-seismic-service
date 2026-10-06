package com.rumi.seismiccorrelation.infrastructure.external.igp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Reads the latest event from the IGP feed over HTTP. The URL must return the JSON shape of
 * IgpSeismicEventResponse; mapping the IGP's own payload is still pending.
 */
@Component
@Profile("!dev")
public class HttpIgpFeedOperation implements IgpFeedOperation {

    private final RestClient restClient;
    private final String latestEventUrl;

    @Autowired
    public HttpIgpFeedOperation(
            RestClient.Builder restClientBuilder,
            @Value("${rumi.igp.latest-event-url}") String latestEventUrl,
            @Value("${rumi.igp.timeout:5s}") Duration timeout
    ) {
        this(restClientBuilder.requestFactory(requestFactory(timeout)).build(), latestEventUrl);
    }

    HttpIgpFeedOperation(RestClient restClient, String latestEventUrl) {
        this.restClient = restClient;
        this.latestEventUrl = latestEventUrl;
    }

    private static SimpleClientHttpRequestFactory requestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return requestFactory;
    }

    @Override
    public IgpSeismicEventResponse fetchLatest() {
        return restClient.get()
                .uri(latestEventUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(IgpSeismicEventResponse.class);
    }
}
