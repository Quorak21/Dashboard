package com.dokkcorp.dashboard.features.stocks.investorab;

import java.util.List;

public record InveBDto(

        String symbol,

        double currentPrice,

        double marketCap,

        double priceChangePercentage24h,

        double totalVolume,

        double lastRefresh,

        List<Double> historyPrices,

        List<Long> historyDays) {

}
