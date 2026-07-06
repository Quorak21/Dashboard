package com.dokkcorp.dashboard.features.assets.model;

import java.util.List;

public record AssetDto(
        String assetId,
        String symbol,
        String displayName,
        AssetType type,
        String currency,
        Double currentPrice,
        Double marketCap,
        Double priceChangePercentage24h,
        Double totalVolume,
        Long lastRefresh,
        PriceSource priceSource,
        MarketStatus marketStatus,
        Integer syncIntervalMinutes,
        List<Double> historyPrices,
        List<Long> historyDays,
        List<Double> livePrices,
        List<Long> liveDays,
        DividendsBlock dividends,
        FundamentalsBlock fundamentals) {

    public static AssetDto error(String assetId, String dbSymbol, String displayName, AssetType type) {
        return new AssetDto(
                assetId,
                dbSymbol,
                displayName,
                type,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                null);
    }
}
