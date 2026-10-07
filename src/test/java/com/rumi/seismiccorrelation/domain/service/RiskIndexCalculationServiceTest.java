package com.rumi.seismiccorrelation.domain.service;

import com.rumi.seismiccorrelation.domain.model.RiskLevel;
import com.rumi.seismiccorrelation.domain.strategy.AiRiskStrategy;
import com.rumi.seismiccorrelation.domain.strategy.RuleBasedRiskStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class RiskIndexCalculationServiceTest {

    @Test
    void calculatesTheRiskIndexWithTheAiStrategy() {
        RiskIndexCalculationService service = new RiskIndexCalculationService(new AiRiskStrategy());

        double riskIndex = service.calculateRiskIndex(0.8);

        assertThat(riskIndex).isCloseTo(0.752, offset(0.0001));
    }

    @Test
    void calculatesTheRiskIndexWithTheRuleBasedStrategy() {
        RiskIndexCalculationService service = new RiskIndexCalculationService(new RuleBasedRiskStrategy());

        double riskIndex = service.calculateRiskIndex(0.8);

        assertThat(riskIndex).isEqualTo(0.8);
    }

    @Test
    void convertsTheRuleBasedScoreIntoALevelAndReportsTheModelVersion() {
        RiskIndexCalculationService service = new RiskIndexCalculationService(new RuleBasedRiskStrategy());

        assertThat(service.calculateLevel(0.1)).isEqualTo(RiskLevel.LOW);
        assertThat(service.calculateLevel(0.5)).isEqualTo(RiskLevel.MEDIUM);
        assertThat(service.calculateLevel(0.9)).isEqualTo(RiskLevel.HIGH);
        assertThat(service.modelVersion()).isEqualTo("rule-based-v1");
    }

    @Test
    void theAiStrategyCanReachCritical() {
        RiskIndexCalculationService service = new RiskIndexCalculationService(new AiRiskStrategy());

        assertThat(service.calculateLevel(0.95)).isEqualTo(RiskLevel.CRITICAL);
        assertThat(service.modelVersion()).isEqualTo("ai-placeholder-v1");
    }
}
