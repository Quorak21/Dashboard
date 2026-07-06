package com.dokkcorp.dashboard.features.assets.model;

public record AssetDefinition(
        String id,
        String displayName,
        AssetProvider provider,
        String symbol,
        String dbSymbol,
        AssetType type,
        String currency,
        MarketHours marketHours,
        SyncConfig sync,
        String scrapeParser) {
}
