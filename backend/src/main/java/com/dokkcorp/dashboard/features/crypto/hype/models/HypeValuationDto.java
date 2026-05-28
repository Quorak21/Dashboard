package com.dokkcorp.dashboard.features.crypto.hype.models;

public record HypeValuationDto(

        Double fdv,
        Double ratioMcapFdv,
        Double ratioOImcap,
        Double dailyVolume,
        Double openInterest,
        Double feesDaily,
        Double feesAnnual,
        Double ratioPriceFees,
        Double stakingApr,
        Double totalStakedHype,
        Double ratioStaked

) {

    public static HypeValuationDto error(String symbol) {
        return new HypeValuationDto(null, null, null, null, null, null, null, null, null, null, null);
    }
}
