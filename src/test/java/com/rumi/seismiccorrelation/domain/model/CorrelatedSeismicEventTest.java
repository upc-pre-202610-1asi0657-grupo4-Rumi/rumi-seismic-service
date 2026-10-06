package com.rumi.seismiccorrelation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelatedSeismicEventTest {

    static final Instant NOW = Instant.parse("2026-10-06T15:32:10Z");

    @Test
    void movesFromDetectedToAnalyzedWithItsRiskIndex() {
        CorrelatedSeismicEvent correlation = CorrelatedSeismicEvent.detect(UUID.randomUUID(), UUID.randomUUID(), NOW);
        RiskIndex riskIndex = new RiskIndex(UUID.randomUUID(), RiskLevel.LOW, NOW, "rule-based-v1", List.of());

        correlation.analyze(riskIndex);

        assertThat(correlation.getStatus()).isEqualTo(EventStatus.ANALYZED);
        assertThat(correlation.getRiskIndex()).contains(riskIndex);
    }

    @Test
    void cannotBeAnalyzedTwice() {
        CorrelatedSeismicEvent correlation = CorrelatedSeismicEvent.detect(UUID.randomUUID(), UUID.randomUUID(), NOW);
        correlation.analyze(new RiskIndex(UUID.randomUUID(), RiskLevel.LOW, NOW, "rule-based-v1", List.of()));

        assertThatThrownBy(() -> correlation.analyze(
                new RiskIndex(UUID.randomUUID(), RiskLevel.LOW, NOW, "rule-based-v1", List.of())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aRiskIndexBelowMediumHasNoAffectedZones() {
        assertThatThrownBy(() -> new RiskIndex(UUID.randomUUID(), RiskLevel.LOW, NOW, "rule-based-v1",
                List.of(new AffectedZoneReport(UUID.randomUUID(), "FLOOR-3-NORTH", 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sameOccurrenceIgnoresTheIgpCodeAndDepth() {
        SeismicEvent first = new SeismicEvent("IGP-1", 5.8, NOW, -12.05, -77.12, 40.0);
        SeismicEvent second = new SeismicEvent("IGP-2", 5.8, NOW, -12.05, -77.12, 35.0);
        SeismicEvent other = new SeismicEvent("IGP-3", 5.8, NOW, -12.06, -77.12, 40.0);

        assertThat(first.isSameOccurrenceAs(second)).isTrue();
        assertThat(first.isSameOccurrenceAs(other)).isFalse();
    }
}
