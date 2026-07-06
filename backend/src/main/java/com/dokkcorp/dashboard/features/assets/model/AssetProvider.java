package com.dokkcorp.dashboard.features.assets.model;

public enum AssetProvider {
    FMP,
    SCRAPE;

    public static AssetProvider fromYaml(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        return switch (value.trim().toLowerCase()) {
            case "fmp" -> FMP;
            case "scrape" -> SCRAPE;
            default -> throw new IllegalArgumentException("unsupported provider: " + value);
        };
    }

    public String providerId() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
