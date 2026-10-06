package com.rumi.seismiccorrelation.domain.model;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public boolean isAtLeast(RiskLevel other) {
        return compareTo(other) >= 0;
    }
}
