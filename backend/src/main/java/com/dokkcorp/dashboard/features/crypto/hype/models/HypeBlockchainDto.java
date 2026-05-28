package com.dokkcorp.dashboard.features.crypto.hype.models;

public record HypeBlockchainDto(

        Double bridgedHype,
        Double liquidStaked,
        Double ratioBridged,
        Double stakedEvmCore) {

    public static HypeBlockchainDto error(String symbol) {
        return new HypeBlockchainDto(null, null, null, null);
    }
}
