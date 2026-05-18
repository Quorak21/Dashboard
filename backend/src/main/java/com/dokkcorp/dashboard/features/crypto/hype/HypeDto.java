package com.dokkcorp.dashboard.features.crypto.hype;

import java.util.List;

// TODO: Refactorisé en mini-DTO pour allégé et separer les données
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

        List<Long> liveDays,

        String circulatingSupply,

        String totalValueLocked,

        String apr,

        String dailyVolume,

        String ratioProvider,

        String openInterest,

        String feesDaily,

        String feesAnnual,

        double volatVolume,

        double volatOpenInterest,

        double volatHlpProvider,

        String stakingApr,

        String maxSupply,

        String circulation100,

        String fdv,

        String ratioMcapFdv,

        String hypeBurned100,

        String ratioPriceFees,

        String ratioOImcap,

        String totalStakedHype,

        String ratioStaked,

        String bridgedHype,

        String ratioBridged,

        String liquidStaked,

        String stakedEvmCore,

        String burned30d,

        String circulating30d,

        String flux30d,

        double burned24h,

        List<Double> fluxBurned,

        List<Double> fluxIssued,

        List<Double> fluxNetFlow,

        List<Long> fluxDays) {

    // Pour gérer les futures erreur et éviter de tout se retaper a la main
    public static HypeDto error(String symbol) {
        return new HypeDto(
                "ERROR", 0.0, 0.0, 0.0, 0.0, (double) System.currentTimeMillis(),
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(),
                "0", "0", "0", "0", "0", "0", "0", "0",
                0.0, 0.0, 0.0,
                "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
                0.0, java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of());
    }
}
