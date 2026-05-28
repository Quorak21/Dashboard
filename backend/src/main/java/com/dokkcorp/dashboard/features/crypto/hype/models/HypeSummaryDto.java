package com.dokkcorp.dashboard.features.crypto.hype.models;

public record HypeSummaryDto(
    String symbol,
    Double currentPrice,
    Double marketCap,
    Double priceChangePercentage24h,
    Double totalVolume,
    Long lastRefresh) {

    public static HypeSummaryDto error(String symbol) {
        return new HypeSummaryDto(symbol, null, null, null, null, null);
    }
}

