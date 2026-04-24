package com.dokkcorp.dashboard.features.crypto.hype;

import java.util.List;

public record HypeDto(

        String symbol,

        double currentPrice,

        double marketCap,

        double priceChangePercentage24h,

        double totalVolume,

        double lastRefresh,

        List<Double> historyPrices,

        List<Long> historyDays,

        List<Double> livePrices,

        List<Long> liveDays) {

}
