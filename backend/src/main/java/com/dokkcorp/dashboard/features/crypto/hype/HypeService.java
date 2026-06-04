package com.dokkcorp.dashboard.features.crypto.hype;

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
import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;

import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

@Service
public class HypeService {

        private final CoinGeckoClient coingeckoclient;
        private final HyperliquidClient hyperliquidClient;
        private final BlockChainClient blockChainClient;
        private final AssetDailyRepository assetDailyRepository;
        private final AssetSnapshotRepository assetSnapshotRepository;
        private final HypeMapper hypeMapper;

        private static final Logger logger = LoggerFactory.getLogger(HypeService.class);

        private final AtomicReference<HypeDto> cachedData = new AtomicReference<>();

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

        // On reçoit la requete, on renvoie le cacho pour eviter de nouvelle requete,
        // s'il est vide, on en créer un
        public HypeDto getLastHypeData() {

                HypeDto data = this.cachedData.get();
                if (data != null) {
                        return data;
                }
                return this.getData();
        }

        public HypeDto getData() {
                ProviderPayload providerPayload = fetchProviderPayload();

                // Si l'un bug, on fait pas le calcul complet
                // (TODO: faire partiellement pour avoir une partie des chiffre)
                // (TODO: Time out + retry)
                if (!providerPayload.isComplete()) {
                        logger.error("Une ou plusieurs API HS");
                        return getCachedOrError();
                }

                // Si tout est OK, on met a jour la DB et on recréer un nouveau cache
                try {
                        AssetDaily newPoint = buildDailyPoint(providerPayload.hyperliquidData(), providerPayload.hypeRaw());
                        initializeHistoryIfMissing();
                        return persistAndCache(newPoint, providerPayload.hyperliquidData(), providerPayload.blockchainData());
                        // Si y'a un problème, c'est que c'est dans la mise à jour DB
                } catch (Exception e) {
                        logger.error("Erreur non prévue, DB ? : {}", e.getMessage());
                        return getCachedOrError();
                }
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
                newPoint.setSymbol("HYPE");
                newPoint.setCurrentPrice(hypeRaw.currentPrice());
                BigDecimal circulatingSupply = safeParseDecimal(hyperliquidData.circulatingSupply(),
                                "hyperliquid.circulatingSupply");
                BigDecimal marketCap = BigDecimal.valueOf(hypeRaw.currentPrice()).multiply(circulatingSupply);
                newPoint.setMarketCap(marketCap.doubleValue());
                newPoint.setPriceChangePercentage24h(hypeRaw.priceChangePercentage24h());
                newPoint.setTotalVolume(hypeRaw.totalVolume());
                newPoint.setLastRefresh(System.currentTimeMillis());
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

        private HypeDto persistAndCache(AssetDaily newPoint, HyperliquidDto hyperliquidData, BlockChainDto blockchainData) {
                AssetDaily savedEntity = this.assetDailyRepository.save(newPoint);
                HypeDto newDto = this.hypeMapper.toDto(savedEntity, hyperliquidData, blockchainData);
                this.cachedData.set(newDto);
                return newDto;
        }

        private HypeDto getCachedOrError() {
                HypeDto data = this.cachedData.get();
                if (data != null) {
                        return data;
                }
                return HypeDto.error("HYPE");
        }

        // Fonction si la DB pour le chart annuel est vide, on la remplit pour avoir une
        // base
        private void initializeHistory() {
                try {
                        CoinGeckoHistoryDto history = this.coingeckoclient.getHistory();
                        List<AssetSnapshot> snapshots = new ArrayList<>();
                        for (int n = 0; n < history.prices().size(); n++) {
                                AssetSnapshot s = new AssetSnapshot();
                                s.setSymbol("HYPE");
                                s.setPrice(history.prices().get(n).get(1));
                                s.setDay(history.prices().get(n).get(0).longValue());
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
                private boolean isComplete() {
                        return this.hyperliquidData != null
                                        && this.blockchainData != null
                                        && this.hypeRaw != null;
                }
        }

}