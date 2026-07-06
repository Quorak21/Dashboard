package com.dokkcorp.dashboard.features.assets.price;

import java.time.Instant;

public record PriceQuote(
        double price,
        String currency,
        Double marketCap,
        Double changePercent24h,
        Double volume,
        Instant fetchedAt) {
}
