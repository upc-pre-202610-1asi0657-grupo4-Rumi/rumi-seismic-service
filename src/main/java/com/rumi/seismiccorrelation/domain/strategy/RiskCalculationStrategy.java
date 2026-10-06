package com.rumi.seismiccorrelation.domain.strategy;

public interface RiskCalculationStrategy {

    double calculateRiskIndex(double normalizedStructuralResponse);

    /** Stored with every risk index, for example rule-based-v1. */
    String modelVersion();
}
