package com.rumi.seismiccorrelation.infrastructure.external.igp;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpIgpFeedOperationTest {

    static final String URL = "http://igp.test/igp/latest-event";

    @Test
    void readsTheLatestEventFromTheFeed() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpIgpFeedOperation operation = new HttpIgpFeedOperation(builder.build(), URL);
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "code": "IGP/CENSIS/RS 2026-0412",
                          "magnitude": 5.8,
                          "magnitudeScale": "Mw",
                          "timestamp": "2026-10-06T15:29:41Z",
                          "latitude": -12.05,
                          "longitude": -77.12,
                          "depthKm": 38.0,
                          "reference": "Callao, 35 km SW"
                        }""", MediaType.APPLICATION_JSON));

        IgpSeismicEventResponse response = operation.fetchLatest();

        assertThat(response).isEqualTo(new IgpSeismicEventResponse(
                "IGP/CENSIS/RS 2026-0412", 5.8, "Mw", Instant.parse("2026-10-06T15:29:41Z"),
                -12.05, -77.12, 38.0, "Callao, 35 km SW"));
        server.verify();
    }
}
