package com.dokkcorp.dashboard.features.stocks.investorab;

import java.util.List;

public record InveBDto(

                String symbol,

                Double currentPrice,

                Double marketCap,

                Double priceChangePercentage24h,

                Double totalVolume,

                Long lastRefresh,

                List<Double> historyPrices,

                List<Long> historyDays,

                List<Double> livePrices,

                List<Long> liveDays) {

        public static InveBDto error(String symbol) {
                return new InveBDto(
                                symbol, null, null, null, null, null,
                                List.of(), List.of(),
                                List.of(), List.of());
        }

}
