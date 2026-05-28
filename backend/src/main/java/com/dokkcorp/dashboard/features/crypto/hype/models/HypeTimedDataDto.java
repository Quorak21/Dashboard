package com.dokkcorp.dashboard.features.crypto.hype.models;

import java.util.List;

public record HypeTimedDataDto(

        Double burned24h,
        Double volatVolume,
        Double volatOpenInterest,
        Double volatHlpProvider,
        Double burned30d,
        Double circulating30d,
        Double flux30d,
        List<Double> fluxBurned,
        List<Double> fluxIssued,
        List<Double> fluxNetFlow,
        List<Long> fluxDays) {

    public static HypeTimedDataDto error(String symbol) {
        return new HypeTimedDataDto(null, null, null, null, null, null, null, List.of(), List.of(), List.of(),
                List.of());
    }
}
