package com.rajlaxmi.jewellers.enums;

public enum GoldPurity {

    GOLD_18K("18K", 75.0, "750", "18 Karat Gold — 75% pure"),
    GOLD_22K("22K", 91.6, "916", "22 Karat Gold — 91.6% pure"),
    GOLD_24K("24K", 99.9, "999", "24 Karat Gold — 99.9% pure");

    private final String displayName;
    private final double purityPercentage;
    private final String bisCode;
    private final String description;

    GoldPurity(String displayName, double purityPercentage, String bisCode, String description) {
        this.displayName = displayName;
        this.purityPercentage = purityPercentage;
        this.bisCode = bisCode;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public double getPurityPercentage() { return purityPercentage; }
    public String getBisCode() { return bisCode; }
    public String getDescription() { return description; }
}
