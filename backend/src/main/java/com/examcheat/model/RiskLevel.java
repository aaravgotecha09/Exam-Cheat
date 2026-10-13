package com.examcheat.model;

/**
 * OOP concept: ENUM with fields and behaviour. Single place that defines the risk thresholds
 * (0-4 NORMAL, 5-9 SUSPICIOUS, 10+ HIGH RISK).
 */
public enum RiskLevel {
    NORMAL("NORMAL", 0), SUSPICIOUS("SUSPICIOUS", 5), HIGH_RISK("HIGH RISK", 10);

    private final String label;
    private final int minScore;

    RiskLevel(String label, int minScore) {
        this.label = label;
        this.minScore = minScore;
    }

    public String getLabel() { return label; }
    public int getMinScore() { return minScore; }

    public static RiskLevel fromScore(int score) {
        if (score >= HIGH_RISK.minScore) return HIGH_RISK;
        if (score >= SUSPICIOUS.minScore) return SUSPICIOUS;
        return NORMAL;
    }
}
