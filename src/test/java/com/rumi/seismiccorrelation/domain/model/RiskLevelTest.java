package com.rumi.seismiccorrelation.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskLevelTest {

    @ParameterizedTest
    @CsvSource({
            "0.0, LOW", "0.2, LOW", "0.3499, LOW",
            "0.35, MEDIUM", "0.5, MEDIUM", "0.6499, MEDIUM",
            "0.65, HIGH", "0.8, HIGH", "0.8499, HIGH",
            "0.85, CRITICAL", "1.0, CRITICAL"
    })
    void convertsAScoreIntoALevel(double score, RiskLevel expected) {
        assertThat(RiskLevel.fromScore(score)).isEqualTo(expected);
    }

    @Test
    void rejectsAScoreOutOfRange() {
        assertThatThrownBy(() -> RiskLevel.fromScore(1.01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RiskLevel.fromScore(-0.01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RiskLevel.fromScore(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void comparesLevels() {
        assertThat(RiskLevel.HIGH.isAtLeast(RiskLevel.MEDIUM)).isTrue();
        assertThat(RiskLevel.MEDIUM.isAtLeast(RiskLevel.MEDIUM)).isTrue();
        assertThat(RiskLevel.LOW.isAtLeast(RiskLevel.MEDIUM)).isFalse();
    }
}
