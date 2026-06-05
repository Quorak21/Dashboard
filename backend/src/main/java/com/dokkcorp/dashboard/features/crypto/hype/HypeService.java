package com.dokkcorp.dashboard.features.crypto.hype;

import java.time.Instant;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dokkcorp.dashboard.providers.coingecko.CoinGeckoClient;
import com.dokkcorp.dashboard.providers.coingecko.CoinGeckoDto;
import com.dokkcorp.dashboard.providers.coingecko.CoinGeckoHistoryDto;

import com.dokkcorp.dashboard.providers.hyperliquid.HyperliquidClient;
import com.dokkcorp.dashboard.providers.hyperliquid.HyperliquidDto;

import com.dokkcorp.dashboard.providers.blockchain.BlockChainClient;
import com.dokkcorp.dashboard.providers.blockchain.BlockChainDto;
import com.dokkcorp.dashboard.features.crypto.hype.maths.HypeConstants;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeBlockchainDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeChartsDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeHlpDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSummaryDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSupplyDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeTimedDataDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeValuationDto;
import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;

import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

@Service
public class HypeService {

        private static final String SYMBOL = "HYPE";

        private final CoinGeckoClient coingeckoclient;
        private final HyperliquidClient hyperliquidClient;
        private final BlockChainClient blockChainClient;
        private final AssetDailyRepository assetDailyRepository;
        private final AssetSnapshotRepository assetSnapshotRepository;
        private final HypeMapper hypeMapper;

        private static final Logger logger = LoggerFactory.getLogger(HypeService.class);

        private final HypeThemeCache themeCache = new HypeThemeCache();

        public HypeService(CoinGeckoClient client, HyperliquidClient hyperliquidClient,
                        BlockChainClient blockChainClient, AssetDailyRepository assetDailyRepository,
                        AssetSnapshotRepository assetSnapshotRepository, HypeMapper hypeMapper) {
                this.coingeckoclient = client;
                this.hyperliquidClient = hyperliquidClient;
                this.blockChainClient = blockChainClient;
                this.assetDailyRepository = assetDailyRepository;
                this.assetSnapshotRepository = assetSnapshotRepository;
                this.hypeMapper = hypeMapper;
        }

        public HypeDto getLastHypeData() {
                HypeDto cached = themeCache.assemble();
                if (cached != null) {
                        return cached;
                }
                return this.getData();
        }

        public HypeDto getData() {
                ProviderPayload providerPayload = fetchProviderPayload();

                List<AssetSnapshot> history = this.hypeMapper.loadHistory();
                List<AssetDaily> daily = this.hypeMapper.loadDaily();

                AssetDaily entityForThemes = daily.isEmpty() ? null : daily.get(daily.size() - 1);
                AssetDaily savedEntity = null;

                if (providerPayload.hypeRaw() != null && providerPayload.hyperliquidData() != null) {
                        try {
                                initializeHistoryIfMissing();
                                AssetDaily newPoint = buildDailyPoint(providerPayload.hyperliquidData(),
                                                providerPayload.hypeRaw());
                                savedEntity = this.assetDailyRepository.save(newPoint);
                                entityForThemes = savedEntity;
                                daily = this.hypeMapper.loadDaily();
                                history = this.hypeMapper.loadHistory();
                        } catch (Exception e) {
                                logger.error("Erreur lors de la persistance AssetDaily : {}", e.getMessage());
                        }
                }

                HypeSummaryDto summary = resolveSummary(savedEntity);
                HypeChartsDto charts = resolveCharts(history, daily);
                HypeTimedDataDto timedData = resolveTimedData(providerPayload.hyperliquidData(), daily, history);
                HypeSupplyDto supply = resolveSupply(providerPayload.hyperliquidData());
                HypeBlockchainDto blockchain = resolveBlockchain(providerPayload.hyperliquidData(),
                                providerPayload.blockchainData());
                HypeHlpDto hlp = resolveHlp(providerPayload.hyperliquidData());
                HypeValuationDto valuation = resolveValuation(providerPayload.hyperliquidData(), entityForThemes,
                                history);

                themeCache.update(summary, charts, timedData, supply, blockchain, hlp, valuation);

                return this.hypeMapper.assemble(summary, charts, timedData, supply, blockchain, hlp, valuation);
        }

        private HypeSummaryDto resolveSummary(AssetDaily savedEntity) {
                if (savedEntity != null) {
                        return this.hypeMapper.buildSummary(savedEntity);
                }
                return fallback(this.themeCache.summary, HypeSummaryDto.error(SYMBOL));
        }

        private HypeChartsDto resolveCharts(List<AssetSnapshot> history, List<AssetDaily> daily) {
                try {
                        return this.hypeMapper.buildCharts(history, daily);
                } catch (Exception e) {
                        logger.warn("Echec calcul charts : {}", e.getMessage());
                        return fallback(this.themeCache.charts, HypeChartsDto.error(SYMBOL));
                }
        }

        private HypeTimedDataDto resolveTimedData(HyperliquidDto hyperliquidData, List<AssetDaily> daily,
                        List<AssetSnapshot> history) {
                if (hyperliquidData == null) {
                        return fallback(this.themeCache.timedData, HypeTimedDataDto.error(SYMBOL));
                }
                try {
                        return this.hypeMapper.buildTimedData(hyperliquidData, daily, history);
                } catch (Exception e) {
                        logger.warn("Echec calcul timedData : {}", e.getMessage());
                        return fallback(this.themeCache.timedData, HypeTimedDataDto.error(SYMBOL));
                }
        }

        private HypeSupplyDto resolveSupply(HyperliquidDto hyperliquidData) {
                if (hyperliquidData == null) {
                        return fallback(this.themeCache.supply, HypeSupplyDto.error(SYMBOL));
                }
                try {
                        return this.hypeMapper.buildSupply(hyperliquidData);
                } catch (Exception e) {
                        logger.warn("Echec calcul supply : {}", e.getMessage());
                        return fallback(this.themeCache.supply, HypeSupplyDto.error(SYMBOL));
                }
        }

        private HypeBlockchainDto resolveBlockchain(HyperliquidDto hyperliquidData, BlockChainDto blockchainData) {
                if (hyperliquidData == null || blockchainData == null) {
                        return fallback(this.themeCache.blockchain, HypeBlockchainDto.error(SYMBOL));
                }
                try {
                        return this.hypeMapper.buildBlockchain(hyperliquidData, blockchainData);
                } catch (Exception e) {
                        logger.warn("Echec calcul blockchain : {}", e.getMessage());
                        return fallback(this.themeCache.blockchain, HypeBlockchainDto.error(SYMBOL));
                }
        }

        private HypeHlpDto resolveHlp(HyperliquidDto hyperliquidData) {
                if (hyperliquidData == null) {
                        return fallback(this.themeCache.hlp, HypeHlpDto.error(SYMBOL));
                }
                try {
                        return this.hypeMapper.buildHlp(hyperliquidData);
                } catch (Exception e) {
                        logger.warn("Echec calcul hlp : {}", e.getMessage());
                        return fallback(this.themeCache.hlp, HypeHlpDto.error(SYMBOL));
                }
        }

        private HypeValuationDto resolveValuation(HyperliquidDto hyperliquidData, AssetDaily entity,
                        List<AssetSnapshot> history) {
                if (hyperliquidData == null || entity == null) {
                        return fallback(this.themeCache.valuation, HypeValuationDto.error(SYMBOL));
                }
                try {
                        return this.hypeMapper.buildValuation(hyperliquidData, entity, history);
                } catch (Exception e) {
                        logger.warn("Echec calcul valuation : {}", e.getMessage());
                        return fallback(this.themeCache.valuation, HypeValuationDto.error(SYMBOL));
                }
        }

        private <T> T fallback(AtomicReference<T> cache, T errorValue) {
                T cached = cache.get();
                return cached != null ? cached : errorValue;
        }

        private ProviderPayload fetchProviderPayload() {
                HyperliquidDto hyperliquidData = null;
                BlockChainDto blockchainData = null;
                CoinGeckoDto hypeRaw = null;

                try {
                        hyperliquidData = this.hyperliquidClient.getHlData();
                } catch (Exception e) {
                        logger.warn("Echec API Hyperliquid : {}", e.getMessage());
                }

                try {
                        blockchainData = this.blockChainClient.getBlockchainData();
                } catch (Exception e) {
                        logger.warn("Echec API Blockchain : {}", e.getMessage());
                }

                try {
                        hypeRaw = this.coingeckoclient.getData()[0];
                } catch (Exception e) {
                        logger.warn("Echec API CoinGecko : {}", e.getMessage());
                }

                return new ProviderPayload(hyperliquidData, blockchainData, hypeRaw);
        }

        private AssetDaily buildDailyPoint(HyperliquidDto hyperliquidData, CoinGeckoDto hypeRaw) {
                AssetDaily newPoint = new AssetDaily();
                newPoint.setSymbol(SYMBOL);
                newPoint.setCurrentPrice(hypeRaw.currentPrice());
                BigDecimal circulatingSupply = safeParseDecimal(hyperliquidData.circulatingSupply(),
                                "hyperliquid.circulatingSupply");
                BigDecimal marketCap = BigDecimal.valueOf(hypeRaw.currentPrice()).multiply(circulatingSupply);
                newPoint.setMarketCap(marketCap.doubleValue());
                newPoint.setPriceChangePercentage24h(hypeRaw.priceChangePercentage24h());
                newPoint.setTotalVolume(hypeRaw.totalVolume());
                newPoint.setLastRefresh(Instant.now());
                newPoint.setBurnedHype(hyperliquidData.hypeBurned());
                newPoint.setCirculatingSupply(hyperliquidData.circulatingSupply());
                BigDecimal feesDaily = safeParseDecimal(hyperliquidData.dailyVolume(), "hyperliquid.dailyVolume")
                                .multiply(BigDecimal.valueOf(HypeConstants.FEE_RATE));
                newPoint.setFeesDaily(String.valueOf(feesDaily.doubleValue()));
                newPoint.setDailyVolume(hyperliquidData.dailyVolume());
                newPoint.setOpenInterest(hyperliquidData.openInterest());
                newPoint.setProviderTvl(hyperliquidData.providerTvl());
                return newPoint;
        }

        private void initializeHistoryIfMissing() {
                if (this.assetSnapshotRepository.findFirstByOrderByDayDesc().isEmpty()) {
                        initializeHistory();
                }
        }

        private void initializeHistory() {
                try {
                        CoinGeckoHistoryDto history = this.coingeckoclient.getHistory();
                        List<AssetSnapshot> snapshots = new ArrayList<>();
                        for (int n = 0; n < history.prices().size(); n++) {
                                AssetSnapshot s = new AssetSnapshot();
                                s.setSymbol(SYMBOL);
                                s.setPrice(history.prices().get(n).get(1));
                                s.setDay(Instant.ofEpochMilli(history.prices().get(n).get(0).longValue()));
                                snapshots.add(s);
                        }
                        this.assetSnapshotRepository.saveAll(snapshots);
                } catch (Exception e) {
                        logger.warn("Historique CoinGecko non initialisé : {}", e.getMessage());
                }
        }

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

        private record ProviderPayload(
                        HyperliquidDto hyperliquidData,
                        BlockChainDto blockchainData,
                        CoinGeckoDto hypeRaw) {
        }

        private static final class HypeThemeCache {

                private final AtomicReference<HypeSummaryDto> summary = new AtomicReference<>();
                private final AtomicReference<HypeChartsDto> charts = new AtomicReference<>();
                private final AtomicReference<HypeTimedDataDto> timedData = new AtomicReference<>();
                private final AtomicReference<HypeSupplyDto> supply = new AtomicReference<>();
                private final AtomicReference<HypeBlockchainDto> blockchain = new AtomicReference<>();
                private final AtomicReference<HypeHlpDto> hlp = new AtomicReference<>();
                private final AtomicReference<HypeValuationDto> valuation = new AtomicReference<>();

                private void update(
                                HypeSummaryDto summary,
                                HypeChartsDto charts,
                                HypeTimedDataDto timedData,
                                HypeSupplyDto supply,
                                HypeBlockchainDto blockchain,
                                HypeHlpDto hlp,
                                HypeValuationDto valuation) {
                        this.summary.set(summary);
                        this.charts.set(charts);
                        this.timedData.set(timedData);
                        this.supply.set(supply);
                        this.blockchain.set(blockchain);
                        this.hlp.set(hlp);
                        this.valuation.set(valuation);
                }

                private HypeDto assemble() {
                        HypeSummaryDto summaryValue = this.summary.get();
                        HypeChartsDto chartsValue = this.charts.get();
                        HypeTimedDataDto timedDataValue = this.timedData.get();
                        HypeSupplyDto supplyValue = this.supply.get();
                        HypeBlockchainDto blockchainValue = this.blockchain.get();
                        HypeHlpDto hlpValue = this.hlp.get();
                        HypeValuationDto valuationValue = this.valuation.get();

                        if (summaryValue == null && chartsValue == null && timedDataValue == null
                                        && supplyValue == null && blockchainValue == null && hlpValue == null
                                        && valuationValue == null) {
                                return null;
                        }

                        return new HypeDto(
                                        summaryValue != null ? summaryValue : HypeSummaryDto.error(SYMBOL),
                                        chartsValue != null ? chartsValue : HypeChartsDto.error(SYMBOL),
                                        timedDataValue != null ? timedDataValue : HypeTimedDataDto.error(SYMBOL),
                                        supplyValue != null ? supplyValue : HypeSupplyDto.error(SYMBOL),
                                        blockchainValue != null ? blockchainValue : HypeBlockchainDto.error(SYMBOL),
                                        hlpValue != null ? hlpValue : HypeHlpDto.error(SYMBOL),
                                        valuationValue != null ? valuationValue : HypeValuationDto.error(SYMBOL));
                }
        }

}
