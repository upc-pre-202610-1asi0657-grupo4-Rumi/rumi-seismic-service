package com.rumi.seismiccorrelation.infrastructure.config;

import com.rumi.seismiccorrelation.domain.service.RiskIndexCalculationService;
import com.rumi.seismiccorrelation.domain.service.SeismicCorrelationService;
import com.rumi.seismiccorrelation.domain.strategy.AiRiskStrategy;
import com.rumi.seismiccorrelation.domain.strategy.RiskCalculationStrategy;
import com.rumi.seismiccorrelation.domain.strategy.RuleBasedRiskStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class SeismicCorrelationConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** rumi.risk.strategy selects the strategy: rule-based (default) or ai. */
    @Bean
    public RiskCalculationStrategy riskCalculationStrategy(@Value("${rumi.risk.strategy:rule-based}") String strategy) {
        return switch (strategy) {
            case "rule-based" -> new RuleBasedRiskStrategy();
            case "ai" -> new AiRiskStrategy();
            default -> throw new IllegalArgumentException("Unknown risk strategy: " + strategy);
        };
    }

    @Bean
    public RiskIndexCalculationService riskIndexCalculationService(RiskCalculationStrategy strategy) {
        return new RiskIndexCalculationService(strategy);
    }

    @Bean
    public SeismicCorrelationService seismicCorrelationService(
            @Value("${rumi.correlation.window:5m}") Duration window,
            @Value("${rumi.correlation.vibration-reference:1.0}") double vibrationReference
    ) {
        return new SeismicCorrelationService(window, vibrationReference);
    }
}
