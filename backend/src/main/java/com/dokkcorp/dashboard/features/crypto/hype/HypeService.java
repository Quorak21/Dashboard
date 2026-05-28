package com.dokkcorp.dashboard.features.crypto.hype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dokkcorp.dashboard.providers.coingecko.CoinGeckoClient;
import com.dokkcorp.dashboard.providers.coingecko.CoinGeckoDto;
import com.dokkcorp.dashboard.providers.coingecko.CoinGeckoHistoryDto;
import com.dokkcorp.dashboard.features.crypto.hype.maths.HypeCalculator;

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

        private final CoinGeckoClient coingeckoclient;
        private final HyperliquidClient hyperliquidClient;
        private final BlockChainClient blockChainClient;
        private final AssetDailyRepository assetDailyRepository;
        private final AssetSnapshotRepository assetSnapshotRepository;
        private final HypeCalculator hypeCalculator;

        private static final Logger logger = LoggerFactory.getLogger(HypeService.class);

        private final AtomicReference<HypeDto> cachedData = new AtomicReference<>();

        public HypeService(CoinGeckoClient client, HyperliquidClient hyperliquidClient,
                        BlockChainClient blockChainClient, AssetDailyRepository assetDailyRepository,
                        AssetSnapshotRepository assetSnapshotRepository, HypeCalculator hypeCalculator) {
                this.coingeckoclient = client;
                this.hyperliquidClient = hyperliquidClient;
                this.blockChainClient = blockChainClient;
                this.assetDailyRepository = assetDailyRepository;
                this.assetSnapshotRepository = assetSnapshotRepository;
                this.hypeCalculator = hypeCalculator;
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
                HyperliquidDto hyperliquidData = null;
                BlockChainDto blockchainData = null;
                CoinGeckoDto hypeRaw = null;

                // On check les API 1 par 1 pour cibler en cas d'erreur Hyperliquid
                try {
                        hyperliquidData = this.hyperliquidClient.getHlData();
                } catch (Exception e) {
                        logger.warn("Echec API Hyperliquid : {}", e.getMessage());
                }
                // Blockchain
                try {
                        blockchainData = this.blockChainClient.getBlockchainData();
                } catch (Exception e) {
                        logger.warn("Echec API Blockchain : {}", e.getMessage());
                }
                // CoinGecko
                try {
                        CoinGeckoDto[] data = this.coingeckoclient.getData();
                        if (data != null && data.length > 0)
                                hypeRaw = data[0];
                } catch (Exception e) {
                        logger.warn("Echec API CoinGecko : {}", e.getMessage());

                }

                // Si l'un bug, on fait pas le calcul complet
                // (TODO: faire partiellement pour avoir une partie des chiffre)
                // (TODO: Time out + retry)
                if (hyperliquidData == null || blockchainData == null || hypeRaw == null) {
                        logger.error("Une ou plusieurs API HS");
                        HypeDto data = this.cachedData.get();
                        return (data != null) ? data : HypeDto.error("HYPE");
                }
                // Si tout est OK, on met a jour la DB et on recréer un nouveau cache
                try {

                        AssetDaily newPoint = new AssetDaily();
                        newPoint.setSymbol("HYPE");
                        newPoint.setCurrentPrice(hypeRaw.currentPrice());
                        double circulatingSupply = safeParseDouble(hyperliquidData.circulatingSupply(),
                                        "hyperliquid.circulatingSupply");
                        newPoint.setMarketCap(hypeRaw.currentPrice() * circulatingSupply);
                        newPoint.setPriceChangePercentage24h(hypeRaw.priceChangePercentage24h());
                        newPoint.setTotalVolume(hypeRaw.totalVolume());
                        newPoint.setLastRefresh(System.currentTimeMillis());
                        newPoint.setBurnedHype(hyperliquidData.hypeBurned());
                        newPoint.setCirculatingSupply(hyperliquidData.circulatingSupply());
                        newPoint.setFeesDaily(
                                        String.valueOf(Double.parseDouble(hyperliquidData.dailyVolume())
                                                        * HypeConstants.FEE_RATE));
                        newPoint.setDailyVolume(hyperliquidData.dailyVolume());
                        newPoint.setOpenInterest(hyperliquidData.openInterest());
                        newPoint.setProviderTvl(hyperliquidData.providerTvl());

                        if (this.assetSnapshotRepository.findFirstByOrderByDayDesc().isEmpty()) {
                                initializeHistory();
                        }

                        AssetDaily savedEntity = this.assetDailyRepository.save(newPoint);
                        // On met à jour le cache avec la nouvelle valeur
                        HypeDto newDto = mapToDto(savedEntity, hyperliquidData, blockchainData);
                        this.cachedData.set(newDto);
                        return newDto;
                        // Si y'a un problème, c'est que c'est dans la mise à jour DB
                } catch (Exception e) {
                        logger.error("Erreur non prévue, DB ? : {}", e.getMessage());
                        // On renvoie le cache
                        HypeDto data = this.cachedData.get();
                        if (data != null) {
                                return data;
                        }
                        // Sinon en renvoie un objet erreur
                        return HypeDto.error("HYPE");
                }
        }

        // Fonction si la DB pour le chart annuel est vide, on la remplit pour avoir une
        // base
        private void initializeHistory() {

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

        }

        // Conversio des mini DTO en DTO final, temporaire pour la migration
        private HypeDto mapToDto(AssetDaily entity, HyperliquidDto hyperliquidData, BlockChainDto blockchainData) {

                // Récupération des données historiques et quotidiennes et mise à l'envers pour
                // avoir les données les plus récentes en premier
                List<AssetSnapshot> history = new ArrayList<>(
                                this.assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("HYPE"));
                Collections.reverse(history);
                List<AssetDaily> daily = new ArrayList<>(
                                this.assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("HYPE"));
                Collections.reverse(daily);

                HypeSummaryDto summaryDto = getSummaryData(entity);
                HypeChartsDto chartsDto = getChartsData(history, daily);
                HypeTimedDataDto timedDataDto = getTimedData(hyperliquidData, daily, history);
                HypeSupplyDto supplyDto = getSupplyData(hyperliquidData);
                HypeBlockchainDto blockchainDto = getBlockchainData(hyperliquidData, blockchainData);
                HypeHlpDto hlpDto = getHlpData(hyperliquidData);
                HypeValuationDto valuationDto = getValuationData(hyperliquidData, entity, history);

                return new HypeDto(
                                summaryDto,
                                chartsDto,
                                timedDataDto,
                                supplyDto,
                                blockchainDto,
                                hlpDto,
                                valuationDto);
        }

        private HypeSummaryDto getSummaryData(AssetDaily entity) {
                return new HypeSummaryDto(
                                entity.getSymbol(),
                                entity.getCurrentPrice(),
                                entity.getMarketCap(),
                                entity.getPriceChangePercentage24h(),
                                entity.getTotalVolume(),
                                entity.getLastRefresh());
        }

        private HypeChartsDto getChartsData(List<AssetSnapshot> history, List<AssetDaily> daily) {

                List<Double> historicalPrice = history.stream().map(AssetSnapshot::getPrice).toList();
                List<Long> historicalDays = history.stream().map(AssetSnapshot::getDay).toList();

                List<Double> livePrice = daily.stream().map(AssetDaily::getCurrentPrice).toList();
                List<Long> liveDay = daily.stream().map(AssetDaily::getLastRefresh).toList();

                return new HypeChartsDto(
                                historicalPrice,
                                historicalDays,
                                livePrice,
                                liveDay);
        }

        private HypeTimedDataDto getTimedData(HyperliquidDto hyperliquidData, List<AssetDaily> daily,
                        List<AssetSnapshot> history) {
                AssetDaily h24 = !daily.isEmpty() ? daily.get(0) : null;
                return this.hypeCalculator.computeTimedData(hyperliquidData, h24, history);
        }

        private HypeSupplyDto getSupplyData(HyperliquidDto hyperliquidData) {
                return this.hypeCalculator.computeSupplyData(hyperliquidData);
        }

        private HypeBlockchainDto getBlockchainData(HyperliquidDto hyperliquidData, BlockChainDto blockchainData) {
                return this.hypeCalculator.computeBlockchainData(blockchainData, hyperliquidData);
        }

        private HypeHlpDto getHlpData(HyperliquidDto hyperliquidData) {
                return this.hypeCalculator.computeHlpData(hyperliquidData);
        }

        private HypeValuationDto getValuationData(HyperliquidDto hyperliquidData, AssetDaily entity,
                        List<AssetSnapshot> history) {
                return this.hypeCalculator.computeValuationData(hyperliquidData, entity, history);
        }

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

}