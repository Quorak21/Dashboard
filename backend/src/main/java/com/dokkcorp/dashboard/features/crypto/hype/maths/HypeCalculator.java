package com.dokkcorp.dashboard.features.crypto.hype.maths;

import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.dokkcorp.dashboard.features.crypto.hype.models.HypeTimedDataDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSupplyDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeBlockchainDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeHlpDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeValuationDto;

import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.providers.hyperliquid.HyperliquidDto;
import com.dokkcorp.dashboard.providers.blockchain.BlockChainDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class HypeCalculator {

    private final Logger logger = LoggerFactory.getLogger(HypeCalculator.class);

    // Timed data Calculation
    public HypeTimedDataDto computeTimedData(HyperliquidDto hyperliquidData, AssetDaily h24,
            List<AssetSnapshot> history) {

        // Switch type variable
        String dailyVolumeLast24h = h24 != null ? h24.getDailyVolume() : null;
        String openInterestLast24h = h24 != null ? h24.getOpenInterest() : null;
        String providerTvlLast24h = h24 != null ? h24.getProviderTvl() : null;

        double hypeBurnedDouble = safeParseDouble(hyperliquidData.hypeBurned(), "hypeBurned");
        double dailyVolumeDouble = safeParseDouble(hyperliquidData.dailyVolume(), "dailyVolume");
        double openInterestDouble = safeParseDouble(hyperliquidData.openInterest(), "openInterest");
        double providerTvlDouble = safeParseDouble(hyperliquidData.providerTvl(), "providerTvl");
        double dailyVolumeLast24hDouble = safeParseDouble(dailyVolumeLast24h, "dailyVolumeLast24h");
        double openInterestLast24hDouble = safeParseDouble(openInterestLast24h, "openInterestLast24h");
        double providerTvlLast24hDouble = safeParseDouble(providerTvlLast24h, "providerTvlLast24h");
        double circulatingSupplyDouble = safeParseDouble(hyperliquidData.circulatingSupply(), "circulatingSupply");

        double burned24h = computeBurned24h(h24, hypeBurnedDouble);
        VolatilityResult volatility = computeVolatility(
                h24,
                dailyVolumeDouble,
                openInterestDouble,
                providerTvlDouble,
                dailyVolumeLast24hDouble,
                openInterestLast24hDouble,
                providerTvlLast24hDouble);
        ThirtyDayResult thirtyDay = computeThirtyDayMetrics(history, hypeBurnedDouble, circulatingSupplyDouble);
        FluxResult flux = computeFluxSeries(history);

        return new HypeTimedDataDto(
                burned24h,
                volatility.volatVolume(),
                volatility.volatOpenInterest(),
                volatility.volatHlpProvider(),
                thirtyDay.burned30d(),
                thirtyDay.circulating30d(),
                thirtyDay.flux30d(),
                flux.fluxBurned(),
                flux.fluxIssued(),
                flux.fluxNetFlow(),
                flux.fluxDays());
    }

    private double computeBurned24h(AssetDaily h24, double hypeBurnedDouble) {
        double oldBurned = h24 != null && h24.getBurnedHype() != null
                ? safeParseDouble(h24.getBurnedHype(), "h24.burnedHype")
                : hypeBurnedDouble;
        return hypeBurnedDouble - oldBurned;
    }

    private VolatilityResult computeVolatility(
            AssetDaily h24,
            double dailyVolumeDouble,
            double openInterestDouble,
            double providerTvlDouble,
            double dailyVolumeLast24hDouble,
            double openInterestLast24hDouble,
            double providerTvlLast24hDouble) {
        double volatVolume = 0;
        double volatOpenInterest = 0;
        double volatHlpProvider = 0;
        try {
            if (h24 != null && dailyVolumeLast24hDouble != 0 && openInterestLast24hDouble != 0
                    && providerTvlLast24hDouble != 0) {
                volatVolume = ((dailyVolumeDouble / dailyVolumeLast24hDouble) - 1) * 100;
                volatOpenInterest = ((openInterestDouble / openInterestLast24hDouble) - 1) * 100;
                volatHlpProvider = ((providerTvlDouble / providerTvlLast24hDouble) - 1) * 100;
            }
        } catch (ArithmeticException | NumberFormatException e) {
            logger.warn("Calcul volatilité impossible (donnée manquante ou division par zéro) : {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur inconnue dans le calcul des volatilités", e);
        }
        return new VolatilityResult(volatVolume, volatOpenInterest, volatHlpProvider);
    }

    private ThirtyDayResult computeThirtyDayMetrics(List<AssetSnapshot> history, double hypeBurnedDouble,
            double circulatingSupplyDouble) {
        double burned30d = 0;
        double circulating30d = 0;
        double flux30d = 0;
        try {
            List<AssetSnapshot> historyWithData = history.stream()
                    .filter(s -> s.getBurnedHype() != null && s.getCirculatingSupply() != null).toList();

            if (!historyWithData.isEmpty()) {
                int size = historyWithData.size();
                int period = Math.min(size, 31);

                AssetSnapshot reference = historyWithData.get(size - period);
                AssetSnapshot latest = historyWithData.get(size - 1);

                double latestBurned = safeParseDouble(latest.getBurnedHype(), "latest.burnedHype");
                double refBurned = safeParseDouble(reference.getBurnedHype(), "reference.burnedHype");
                double latestCirc = safeParseDouble(latest.getCirculatingSupply(), "latest.circulatingSupply");
                double refCirc = safeParseDouble(reference.getCirculatingSupply(), "reference.circulatingSupply");

                double burnedAvg;
                double circAvg;

                if (period > 1) {
                    burnedAvg = safeDivide(latestBurned - refBurned, period - 1, "burnedAvg period");
                    circAvg = safeDivide(latestCirc - refCirc, period - 1, "circAvg period");
                } else {
                    burnedAvg = hypeBurnedDouble - latestBurned;
                    circAvg = circulatingSupplyDouble - latestCirc;
                }

                burned30d = burnedAvg;
                circulating30d = circAvg + burnedAvg;
                flux30d = circAvg;
            }
        } catch (ArithmeticException | NumberFormatException e) {
            logger.warn("Calcul impossible (donnée manquante ou division par zéro) : {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur dans le calcul des données 30j ", e);
        }
        return new ThirtyDayResult(burned30d, circulating30d, flux30d);
    }

    private FluxResult computeFluxSeries(List<AssetSnapshot> history) {
        List<Double> fluxBurned = new ArrayList<>();
        List<Double> fluxIssued = new ArrayList<>();
        List<Double> fluxNetFlow = new ArrayList<>();
        List<Long> fluxDays = new ArrayList<>();

        try {
            for (int i = 1; i < history.size(); i++) {
                AssetSnapshot current = history.get(i);
                AssetSnapshot previous = history.get(i - 1);

                if (current.getBurnedHype() != null && previous.getBurnedHype() != null
                        && current.getCirculatingSupply() != null
                        && previous.getCirculatingSupply() != null) {
                    double burnedDelta = safeParseDouble(current.getBurnedHype(), "current.burnedHype")
                            - safeParseDouble(previous.getBurnedHype(), "previous.burnedHype");
                    double circulatingDelta = safeParseDouble(current.getCirculatingSupply(),
                            "current.circulatingSupply")
                            - safeParseDouble(previous.getCirculatingSupply(), "previous.circulatingSupply");
                    double issuedDelta = circulatingDelta + burnedDelta;
                    double netFlow = circulatingDelta;
                    fluxBurned.add(burnedDelta);
                    fluxIssued.add(issuedDelta);
                    fluxNetFlow.add(netFlow);
                    fluxDays.add(current.getDay());
                }
            }
        } catch (ArithmeticException | NumberFormatException e) {
            logger.warn("Calcul impossible (donnée manquante ou division par zéro) : {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur inconnue dans le calcul de la chart des flux", e);
        }
        return new FluxResult(fluxBurned, fluxIssued, fluxNetFlow, fluxDays);
    }

    private record VolatilityResult(double volatVolume, double volatOpenInterest, double volatHlpProvider) {
    }

    private record ThirtyDayResult(double burned30d, double circulating30d, double flux30d) {
    }

    private record FluxResult(List<Double> fluxBurned, List<Double> fluxIssued, List<Double> fluxNetFlow,
            List<Long> fluxDays) {
    }

    // Supply data Calculation
    public HypeSupplyDto computeSupplyData(HyperliquidDto hyperliquidData) {

        // Changement de type variable pour les calculs
        double circulatingSupply = safeParseDouble(hyperliquidData.circulatingSupply(), "circulatingSupply");
        double maxSupply = safeParseDouble(hyperliquidData.maxSupply(), "maxSupply");
        double hypeBurned = safeParseDouble(hyperliquidData.hypeBurned(), "hypeBurned");

        // Hype brulé % pourcentage
        double hypeBurned100 = (hypeBurned / HypeConstants.TOTAL_SUPPLY) * 100;
        // % en circulation
        double circulating100 = maxSupply != 0 ? (circulatingSupply / maxSupply) * 100 : 0;

        return new HypeSupplyDto(circulatingSupply, maxSupply, hypeBurned100, circulating100);
    }

    // Blockchain data Calculation
    public HypeBlockchainDto computeBlockchainData(BlockChainDto blockchainData, HyperliquidDto hyperliquidData) {
        double bridgedHype = safeParseDouble(blockchainData.bridgedHype(), "bridgedHype");
        double liquidStaked = safeParseDouble(blockchainData.liquidStaked(), "liquidStaked");
        double circulatingSupply = safeParseDouble(hyperliquidData.circulatingSupply(), "circulatingSupply");

        // % bridged hype sur EVM
        double ratioBridged = safeDivide(bridgedHype, circulatingSupply, "ratioBridged circulatingSupply") * 100;
        // % staké evm vs core
        double stakedEvmCore = safeDivide(liquidStaked, circulatingSupply, "stakedEvmCore circulatingSupply") * 100;

        return new HypeBlockchainDto(bridgedHype, liquidStaked, ratioBridged, stakedEvmCore);
    }

    // HLP data Calculation
    public HypeHlpDto computeHlpData(HyperliquidDto hyperliquidData) {
        double providerTvl = safeParseDouble(hyperliquidData.providerTvl(), "providerTvl");
        double providerApr = safeParseDouble(hyperliquidData.providerApr(), "providerApr");

        // Volume TVL HLP
        double volume = safeParseDouble(hyperliquidData.dailyVolume(), "dailyVolume");
        double ratioVolTvl = safeDivide(volume, providerTvl, "ratioVolTvl providerTvl");

        return new HypeHlpDto(providerTvl, providerApr, ratioVolTvl);
    }

    // Valuation data Calculation
    public HypeValuationDto computeValuationData(HyperliquidDto hyperliquidData, AssetDaily entity,
            List<AssetSnapshot> history) {

        double maxSupply = safeParseDouble(hyperliquidData.maxSupply(), "maxSupply");
        double totalStakedHype = safeParseDouble(hyperliquidData.totalStakedHype(), "totalStakedHype");

        // FDV
        double fdv = maxSupply * entity.getCurrentPrice();

        // Ratio Mcap/FDV
        double ratioMcapFdv = safeDivide(entity.getMarketCap(), fdv, "ratioMcapFdv fdv");

        // Ratio OI/Mcap
        double ratioOImcap = safeDivide(
                safeParseDouble(hyperliquidData.openInterest(), "openInterest"),
                entity.getMarketCap(),
                "ratioOImcap marketCap");

        // Estimation fees
        double volume = safeParseDouble(hyperliquidData.dailyVolume(), "dailyVolume");
        double feesDaily = volume * HypeConstants.FEE_RATE;
        double averageDailyFees = history.stream()
                .filter(snapshot -> snapshot.getFees24h() != null)
                .mapToDouble(AssetSnapshot::getFees24h)
                .average()
                .orElse(feesDaily);
        double feesAnnual = averageDailyFees * 365;

        // Staking APR
        double stakingApr = safeParseDouble(hyperliquidData.stakingApr(), "stakingApr");

        // Open Interest
        double openInterest = safeParseDouble(hyperliquidData.openInterest(), "openInterest");

        // Ratio Price to Fees
        double ratioPriceFees = safeDivide(entity.getMarketCap(), feesAnnual, "ratioPriceFees feesAnnual");

        // Ratio % staké
        double ratioStaked = safeDivide(totalStakedHype, maxSupply, "ratioStaked maxSupply") * 100;

        return new HypeValuationDto(fdv, ratioMcapFdv, ratioOImcap, volume, openInterest, feesDaily, feesAnnual,
                ratioPriceFees, stakingApr, totalStakedHype, ratioStaked);
    }

    // Fonctions de sécurisation des données
    private double safeParseDouble(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            logger.warn("Valeur manquante pour {}", fieldName);
            return 0d;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logger.warn("Valeur invalide pour {} : {}", fieldName, value);
            return 0d;
        }
    }

    private double safeDivide(double numerator, double denominator, String operationName) {
        if (denominator == 0d) {
            logger.warn("Division impossible pour {}: denominateur a 0", operationName);
            return 0d;
        }
        return numerator / denominator;
    }
}
