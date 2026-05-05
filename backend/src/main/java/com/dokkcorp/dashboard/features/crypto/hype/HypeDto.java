package com.dokkcorp.dashboard.features.crypto.hype;

import java.util.List;

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

                double volatFees,

                double volatHlpProvider,

                String stakingApr,

                String maxSupply,

                String circulation100,

                String fdv,

                String ratioMcapFdv,

                String hypeBurned,

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

                double burned24h) {

}
