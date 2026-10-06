package com.rumi.seismiccorrelation.domain.model;

/**
 * Risk level of a building after a seismic event. A strategy score in [0, 1] maps to a level:
 *
 * <pre>
 * score in [0.00, 0.35) -> LOW
 * score in [0.35, 0.65) -> MEDIUM
 * score in [0.65, 0.85) -> HIGH
 * score in [0.85, 1.00] -> CRITICAL
 * </pre>
 *
 * These are demonstration thresholds, not calibrated structural limits. With them,
 * RuleBasedRiskStrategy (0.2 / 0.5 / 0.8) yields LOW / MEDIUM / HIGH; CRITICAL is only
 * reachable with a continuous strategy such as AiRiskStrategy.
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static final double MEDIUM_THRESHOLD = 0.35;
    public static final double HIGH_THRESHOLD = 0.65;
    public static final double CRITICAL_THRESHOLD = 0.85;

    public static RiskLevel fromScore(double score) {
        if (!Double.isFinite(score) || score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("Risk score must be between 0 and 1");
        }
        if (score >= CRITICAL_THRESHOLD) {
            return CRITICAL;
        }
        if (score >= HIGH_THRESHOLD) {
            return HIGH;
        }
        if (score >= MEDIUM_THRESHOLD) {
            return MEDIUM;
        }
        return LOW;
    }

    public boolean isAtLeast(RiskLevel other) {
        return compareTo(other) >= 0;
    }
}
