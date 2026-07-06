package com.dokkcorp.dashboard.features.assets.model;

public record RegisteredAssetDto(
        String id,
        String displayName,
        String type,
        String currency) {
}
