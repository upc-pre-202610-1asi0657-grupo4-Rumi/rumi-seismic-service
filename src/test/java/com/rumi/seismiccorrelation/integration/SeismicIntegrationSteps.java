package com.rumi.seismiccorrelation.integration;

import com.rumi.seismiccorrelation.domain.model.AffectedZoneReport;
import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.EventStatus;
import com.rumi.seismiccorrelation.domain.model.RiskIndex;
import com.rumi.seismiccorrelation.domain.model.RiskLevel;
import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.CorrelatedSeismicEventRepository;
import com.rumi.seismiccorrelation.domain.repository.SeismicEventRepository;
import com.rumi.seismiccorrelation.infrastructure.web.dto.AffectedZoneResponse;
import com.rumi.seismiccorrelation.infrastructure.web.dto.RiskIndexResponse;
import com.rumi.seismiccorrelation.infrastructure.web.dto.SeismicEventResponse;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
public class SeismicIntegrationSteps {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("seismic_test")
            .withUsername("test")
            .withPassword("test");

    private static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");
    private static final UUID EVENT_ID = UUID.fromString("1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9");
    private static final UUID CORRELATION_ID = UUID.fromString("9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4");
    private static final UUID RISK_INDEX_ID = UUID.fromString("c3a1e8d2-6b7f-4d09-9a21-5e8f7b6c4d3a");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        properties.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
        properties.add("spring.rabbitmq.listener.direct.auto-startup", () -> "false");
    }

    @Autowired
    private SeismicEventRepository seismicEventRepository;

    @Autowired
    private CorrelatedSeismicEventRepository correlatedEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestRestTemplate http;

    private ResponseEntity<List<SeismicEventResponse>> eventListResponse;
    private ResponseEntity<RiskIndexResponse> riskIndexResponse;
    private ResponseEntity<List<AffectedZoneResponse>> affectedZonesResponse;

    @Before
    public void clearDatabase() {
        jdbcTemplate.update("DELETE FROM affected_zone_reports");
        jdbcTemplate.update("DELETE FROM risk_indexes");
        jdbcTemplate.update("DELETE FROM correlated_seismic_events");
        jdbcTemplate.update("DELETE FROM seismic_events");
    }

    @Given("PostgreSQL contains seismic events inside and outside a date range")
    public void seedEvents() {
        saveEvent("a1b2c3d4-1111-4111-8111-111111111111", "2026-10-06T14:59:59Z", 4.2);
        saveEvent("a1b2c3d4-2222-4222-8222-222222222222", "2026-10-06T15:15:00Z", 5.1);
        saveEvent("a1b2c3d4-3333-4333-8333-333333333333", "2026-10-06T15:45:00Z", 5.8);
        saveEvent("a1b2c3d4-4444-4444-8444-444444444444", "2026-10-06T16:00:01Z", 4.7);
    }

    @When("I request seismic events for that date range")
    public void requestEvents() {
        eventListResponse = http.exchange(
                "/api/v1/seismic-events?from=2026-10-06T15:00:00Z&to=2026-10-06T16:00:00Z",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() { }
        );
    }

    @Then("I receive only the stored events in the range newest first")
    public void verifyEvents() {
        assertThat(eventListResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(eventListResponse.getBody()).hasSize(2);
        assertThat(eventListResponse.getBody()).extracting(SeismicEventResponse::occurredAt)
                .containsExactly(
                        Instant.parse("2026-10-06T15:45:00Z"),
                        Instant.parse("2026-10-06T15:15:00Z")
                );
    }

    @Given("PostgreSQL contains a risk index with affected zones for the building")
    public void seedRiskIndex() {
        seismicEventRepository.save(new SeismicEvent(
                EVENT_ID.toString(), 5.8, Instant.parse("2026-10-06T15:29:41Z"), -12.05, -77.12, 35
        ));
        CorrelatedSeismicEvent correlation = new CorrelatedSeismicEvent(
                CORRELATION_ID, EVENT_ID, BUILDING_ID, EventStatus.DETECTED,
                Instant.parse("2026-10-06T15:30:00Z"), null
        );
        correlation.analyze(new RiskIndex(
                RISK_INDEX_ID,
                RiskLevel.HIGH,
                Instant.parse("2026-10-06T15:32:10Z"),
                "rule-based-v1",
                List.of(
                        new AffectedZoneReport(UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                                "FLOOR-3-NORTH", 1),
                        new AffectedZoneReport(UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                                "FLOOR-2-SOUTH", 2)
                )
        ));
        correlatedEventRepository.save(correlation);
    }

    @When("I request the latest risk index for the building")
    public void requestRiskIndex() {
        riskIndexResponse = http.getForEntity(
                "/api/v1/buildings/" + BUILDING_ID + "/risk-indexes/latest", RiskIndexResponse.class
        );
        affectedZonesResponse = http.exchange(
                "/api/v1/risk-indexes/" + RISK_INDEX_ID + "/affected-zones",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() { }
        );
    }

    @Then("I receive the stored risk level and its affected zones ordered by severity")
    public void verifyRiskIndex() {
        assertThat(riskIndexResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(riskIndexResponse.getBody().id()).isEqualTo(RISK_INDEX_ID);
        assertThat(riskIndexResponse.getBody().level()).isEqualTo(RiskLevel.HIGH);
        assertThat(affectedZonesResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(affectedZonesResponse.getBody()).extracting(AffectedZoneResponse::zone)
                .containsExactly("FLOOR-3-NORTH", "FLOOR-2-SOUTH");
        assertThat(affectedZonesResponse.getBody()).extracting(AffectedZoneResponse::severityRank)
                .containsExactly(1, 2);
    }

    private void saveEvent(String id, String occurredAt, double magnitude) {
        seismicEventRepository.save(new SeismicEvent(
                id, magnitude, Instant.parse(occurredAt), -12.05, -77.12, 35
        ));
    }
}
