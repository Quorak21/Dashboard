package com.dokkcorp.dashboard.features.stocks.investorab;

import java.util.List;

// TODO: Refactorisé en mini-DTO pour allégé et separer les données
public record InveBDto(

                String symbol,

                double currentPrice,

                double marketCap,

                double priceChangePercentage24h,

                double totalVolume,

                double lastRefresh,

                List<Double> historyPrices,

                List<Long> historyDays) {

        public static InveBDto error(String symbol) {
                return new InveBDto(
                                "ERROR", 0.0, 0.0, 0.0, 0.0, (double) System.currentTimeMillis(),
                                java.util.List.of(), java.util.List.of());
        }

}
