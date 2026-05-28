package com.dokkcorp.dashboard.features.crypto.hype.models;

public record HypeHlpDto(

        Double providerTvl,
        Double providerApr,
        Double ratioProvider

) {

    public static HypeHlpDto error(String symbol) {
        return new HypeHlpDto(null, null, null);
    }
}
