package com.dokkcorp.dashboard.features.crypto.hype.models;

import java.util.List;

public record HypeChartsDto(

        List<Double> historyPrices,

        List<Long> historyDays,

        List<Double> livePrices,

        List<Long> liveDays,

        List<Double> activityVolume,

        List<Double> activityOpenInterest,

        List<Long> activityDays) {

    public static HypeChartsDto error(String symbol) {
        return new HypeChartsDto(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }
}
