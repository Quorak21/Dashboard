package com.dokkcorp.dashboard.features.assets.model;

public enum AssetType {
    STOCK,
    REIT,
    ETF,
    TRUST;

    public static AssetType fromYaml(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        return AssetType.valueOf(value.trim().toUpperCase());
    }
}
