package com.dokkcorp.dashboard.features.crypto.hype.maths;

import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;

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

    private static final int DIVISION_SCALE = 12;
    private static final int OUTPUT_SCALE = 8;
    private static final BigDecimal BD_100 = BigDecimal.valueOf(100);
    private static final BigDecimal BD_365 = BigDecimal.valueOf(365);

    private final Logger logger = LoggerFactory.getLogger(HypeCalculator.class);

    // Timed data Calculation
    public HypeTimedDataDto computeTimedData(HyperliquidDto hyperliquidData, AssetDaily h24,
            List<AssetSnapshot> history) {

        // Switch type variable
        String dailyVolumeLast24h = h24 != null ? h24.getDailyVolume() : null;
        String openInterestLast24h = h24 != null ? h24.getOpenInterest() : null;
        String providerTvlLast24h = h24 != null ? h24.getProviderTvl() : null;

        BigDecimal hypeBurned = safeParseDecimal(hyperliquidData.hypeBurned(), "hypeBurned");
        BigDecimal dailyVolume = safeParseDecimal(hyperliquidData.dailyVolume(), "dailyVolume");
        BigDecimal openInterest = safeParseDecimal(hyperliquidData.openInterest(), "openInterest");
        BigDecimal providerTvl = safeParseDecimal(hyperliquidData.providerTvl(), "providerTvl");
        BigDecimal dailyVolumeLast24hValue = safeParseDecimal(dailyVolumeLast24h, "dailyVolumeLast24h");
        BigDecimal openInterestLast24hValue = safeParseDecimal(openInterestLast24h, "openInterestLast24h");
        BigDecimal providerTvlLast24hValue = safeParseDecimal(providerTvlLast24h, "providerTvlLast24h");
        BigDecimal circulatingSupply = safeParseDecimal(hyperliquidData.circulatingSupply(), "circulatingSupply");

        BigDecimal burned24h = computeBurned24h(h24, hypeBurned);
        VolatilityResult volatility = computeVolatility(
                h24,
                dailyVolume,
                openInterest,
                providerTvl,
                dailyVolumeLast24hValue,
                openInterestLast24hValue,
                providerTvlLast24hValue);
        ThirtyDayResult thirtyDay = computeThirtyDayMetrics(history, hypeBurned, circulatingSupply);
        FluxResult flux = computeFluxSeries(history);

        return new HypeTimedDataDto(
                toDouble(burned24h),
                toDouble(volatility.volatVolume()),
                toDouble(volatility.volatOpenInterest()),
                toDouble(volatility.volatHlpProvider()),
                toDouble(thirtyDay.burned30d()),
                toDouble(thirtyDay.circulating30d()),
                toDouble(thirtyDay.flux30d()),
                flux.fluxBurned(),
                flux.fluxIssued(),
                flux.fluxNetFlow(),
                flux.fluxDays());
    }

    private BigDecimal computeBurned24h(AssetDaily h24, BigDecimal hypeBurned) {
        BigDecimal oldBurned = h24 != null && h24.getBurnedHype() != null
                ? safeParseDecimal(h24.getBurnedHype(), "h24.burnedHype")
                : hypeBurned;
        return hypeBurned.subtract(oldBurned);
    }

    private VolatilityResult computeVolatility(
            AssetDaily h24,
            BigDecimal dailyVolume,
            BigDecimal openInterest,
            BigDecimal providerTvl,
            BigDecimal dailyVolumeLast24hValue,
            BigDecimal openInterestLast24hValue,
            BigDecimal providerTvlLast24hValue) {
        BigDecimal volatVolume = BigDecimal.ZERO;
        BigDecimal volatOpenInterest = BigDecimal.ZERO;
        BigDecimal volatHlpProvider = BigDecimal.ZERO;
        try {
            if (h24 != null && isNonZero(dailyVolumeLast24hValue) && isNonZero(openInterestLast24hValue)
                    && isNonZero(providerTvlLast24hValue)) {
                volatVolume = safeDivide(dailyVolume, dailyVolumeLast24hValue, "volatVolume")
                        .subtract(BigDecimal.ONE)
                        .multiply(BD_100);
                volatOpenInterest = safeDivide(openInterest, openInterestLast24hValue, "volatOpenInterest")
                        .subtract(BigDecimal.ONE)
                        .multiply(BD_100);
                volatHlpProvider = safeDivide(providerTvl, providerTvlLast24hValue, "volatHlpProvider")
                        .subtract(BigDecimal.ONE)
                        .multiply(BD_100);
            }
        } catch (ArithmeticException | NumberFormatException e) {
            logger.warn("Calcul volatilité impossible (donnée manquante ou division par zéro) : {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur inconnue dans le calcul des volatilités", e);
        }
        return new VolatilityResult(volatVolume, volatOpenInterest, volatHlpProvider);
    }

    private ThirtyDayResult computeThirtyDayMetrics(List<AssetSnapshot> history, BigDecimal hypeBurned,
            BigDecimal circulatingSupply) {
        BigDecimal burned30d = BigDecimal.ZERO;
        BigDecimal circulating30d = BigDecimal.ZERO;
        BigDecimal flux30d = BigDecimal.ZERO;
        try {
            List<AssetSnapshot> historyWithData = history.stream()
                    .filter(s -> s.getBurnedHype() != null && s.getCirculatingSupply() != null).toList();

            if (!historyWithData.isEmpty()) {
                int size = historyWithData.size();
                int period = Math.min(size, 31);

                AssetSnapshot reference = historyWithData.get(size - period);
                AssetSnapshot latest = historyWithData.get(size - 1);

                BigDecimal latestBurned = safeParseDecimal(latest.getBurnedHype(), "latest.burnedHype");
                BigDecimal refBurned = safeParseDecimal(reference.getBurnedHype(), "reference.burnedHype");
                BigDecimal latestCirc = safeParseDecimal(latest.getCirculatingSupply(), "latest.circulatingSupply");
                BigDecimal refCirc = safeParseDecimal(reference.getCirculatingSupply(), "reference.circulatingSupply");

                BigDecimal burnedAvg;
                BigDecimal circAvg;

                if (period > 1) {
                    BigDecimal periodDivisor = BigDecimal.valueOf(period - 1L);
                    burnedAvg = safeDivide(latestBurned.subtract(refBurned), periodDivisor, "burnedAvg period");
                    circAvg = safeDivide(latestCirc.subtract(refCirc), periodDivisor, "circAvg period");
                } else {
                    burnedAvg = hypeBurned.subtract(latestBurned);
                    circAvg = circulatingSupply.subtract(latestCirc);
                }

                burned30d = burnedAvg;
                circulating30d = circAvg.add(burnedAvg);
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
                    BigDecimal burnedDelta = safeParseDecimal(current.getBurnedHype(), "current.burnedHype")
                            .subtract(safeParseDecimal(previous.getBurnedHype(), "previous.burnedHype"));
                    BigDecimal circulatingDelta = safeParseDecimal(current.getCirculatingSupply(),
                            "current.circulatingSupply")
                            .subtract(safeParseDecimal(previous.getCirculatingSupply(), "previous.circulatingSupply"));
                    BigDecimal issuedDelta = circulatingDelta.add(burnedDelta);
                    BigDecimal netFlow = circulatingDelta;
                    fluxBurned.add(toDouble(burnedDelta));
                    fluxIssued.add(toDouble(issuedDelta));
                    fluxNetFlow.add(toDouble(netFlow));
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

    private record VolatilityResult(BigDecimal volatVolume, BigDecimal volatOpenInterest, BigDecimal volatHlpProvider) {
    }

    private record ThirtyDayResult(BigDecimal burned30d, BigDecimal circulating30d, BigDecimal flux30d) {
    }

    private record FluxResult(List<Double> fluxBurned, List<Double> fluxIssued, List<Double> fluxNetFlow,
            List<Long> fluxDays) {
    }

    // Supply data Calculation
    public HypeSupplyDto computeSupplyData(HyperliquidDto hyperliquidData) {

        // Changement de type variable pour les calculs
        BigDecimal circulatingSupply = safeParseDecimal(hyperliquidData.circulatingSupply(), "circulatingSupply");
        BigDecimal maxSupply = safeParseDecimal(hyperliquidData.maxSupply(), "maxSupply");
        BigDecimal hypeBurned = safeParseDecimal(hyperliquidData.hypeBurned(), "hypeBurned");

        // Hype brulé % pourcentage
        BigDecimal hypeBurned100 = safeDivide(hypeBurned, BigDecimal.valueOf(HypeConstants.TOTAL_SUPPLY), "hypeBurned100")
                .multiply(BD_100);
        // % en circulation
        BigDecimal circulating100 = safeDivide(circulatingSupply, maxSupply, "circulating100").multiply(BD_100);

        return new HypeSupplyDto(toDouble(circulatingSupply), toDouble(maxSupply), toDouble(hypeBurned100),
                toDouble(circulating100));
    }

    // Blockchain data Calculation
    public HypeBlockchainDto computeBlockchainData(BlockChainDto blockchainData, HyperliquidDto hyperliquidData) {
        BigDecimal bridgedHype = safeParseDecimal(blockchainData.bridgedHype(), "bridgedHype");
        BigDecimal liquidStaked = safeParseDecimal(blockchainData.liquidStaked(), "liquidStaked");
        BigDecimal circulatingSupply = safeParseDecimal(hyperliquidData.circulatingSupply(), "circulatingSupply");

        // % bridged hype sur EVM
        BigDecimal ratioBridged = safeDivide(bridgedHype, circulatingSupply, "ratioBridged circulatingSupply")
                .multiply(BD_100);
        // % staké evm vs core
        BigDecimal stakedEvmCore = safeDivide(liquidStaked, circulatingSupply, "stakedEvmCore circulatingSupply")
                .multiply(BD_100);

        return new HypeBlockchainDto(toDouble(bridgedHype), toDouble(liquidStaked), toDouble(ratioBridged),
                toDouble(stakedEvmCore));
    }

    // HLP data Calculation
    public HypeHlpDto computeHlpData(HyperliquidDto hyperliquidData) {
        BigDecimal providerTvl = safeParseDecimal(hyperliquidData.providerTvl(), "providerTvl");
        BigDecimal providerApr = safeParseDecimal(hyperliquidData.providerApr(), "providerApr");

        // Volume TVL HLP
        BigDecimal volume = safeParseDecimal(hyperliquidData.dailyVolume(), "dailyVolume");
        BigDecimal ratioVolTvl = safeDivide(volume, providerTvl, "ratioVolTvl providerTvl");

        return new HypeHlpDto(toDouble(providerTvl), toDouble(providerApr), toDouble(ratioVolTvl));
    }

    // Valuation data Calculation
    public HypeValuationDto computeValuationData(HyperliquidDto hyperliquidData, AssetDaily entity,
            List<AssetSnapshot> history) {

        BigDecimal maxSupply = safeParseDecimal(hyperliquidData.maxSupply(), "maxSupply");
        BigDecimal totalStakedHype = safeParseDecimal(hyperliquidData.totalStakedHype(), "totalStakedHype");

        // FDV
        BigDecimal fdv = maxSupply.multiply(toBigDecimal(entity.getCurrentPrice()));

        // Ratio Mcap/FDV
        BigDecimal marketCap = toBigDecimal(entity.getMarketCap());
        BigDecimal ratioMcapFdv = safeDivide(marketCap, fdv, "ratioMcapFdv fdv");

        // Ratio OI/Mcap
        BigDecimal ratioOImcap = safeDivide(
                safeParseDecimal(hyperliquidData.openInterest(), "openInterest"),
                marketCap,
                "ratioOImcap marketCap");

        // Estimation fees
        BigDecimal volume = safeParseDecimal(hyperliquidData.dailyVolume(), "dailyVolume");
        BigDecimal feesDaily = volume.multiply(BigDecimal.valueOf(HypeConstants.FEE_RATE));
        BigDecimal averageDailyFees = history.stream()
                .filter(snapshot -> snapshot.getFees24h() != null)
                .map(snapshot -> toBigDecimal(snapshot.getFees24h()))
                .reduce(BigDecimal::add)
                .map(sum -> safeDivide(sum, BigDecimal.valueOf(history.stream().filter(snapshot -> snapshot.getFees24h() != null).count()), "averageDailyFees"))
                .orElse(feesDaily);
        BigDecimal feesAnnual = averageDailyFees.multiply(BD_365);

        // Staking APR
        BigDecimal stakingApr = safeParseDecimal(hyperliquidData.stakingApr(), "stakingApr");

        // Open Interest
        BigDecimal openInterest = safeParseDecimal(hyperliquidData.openInterest(), "openInterest");

        // Ratio Price to Fees
        BigDecimal ratioPriceFees = safeDivide(marketCap, feesAnnual, "ratioPriceFees feesAnnual");

        // Ratio % staké
        BigDecimal ratioStaked = safeDivide(totalStakedHype, maxSupply, "ratioStaked maxSupply").multiply(BD_100);

        return new HypeValuationDto(toDouble(fdv), toDouble(ratioMcapFdv), toDouble(ratioOImcap), toDouble(volume),
                toDouble(openInterest), toDouble(feesDaily), toDouble(feesAnnual), toDouble(ratioPriceFees),
                toDouble(stakingApr), toDouble(totalStakedHype), toDouble(ratioStaked));
    }

    // Fonctions de sécurisation des données
    private BigDecimal safeParseDecimal(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            logger.warn("Valeur manquante pour {}", fieldName);
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            logger.warn("Valeur invalide pour {} : {}", fieldName, value);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator, String operationName) {
        if (!isNonZero(denominator)) {
            logger.warn("Division impossible pour {}: denominateur a 0", operationName);
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, DIVISION_SCALE, RoundingMode.HALF_UP);
    }

    private boolean isNonZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) != 0;
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private double toDouble(BigDecimal value) {
        if (value == null) {
            return 0d;
        }
        return value.setScale(OUTPUT_SCALE, RoundingMode.HALF_UP).doubleValue();
    }
}
