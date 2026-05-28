package com.dokkcorp.dashboard.features.crypto.hype.models;

public record HypeSupplyDto(

        Double circulatingSupply,
        Double maxSupply,
        Double hypeBurned100,
        Double circulating100

) {

    public static HypeSupplyDto error(String symbol) {
        return new HypeSupplyDto(null, null, null, null);
    }
}
