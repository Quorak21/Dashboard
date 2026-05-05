package com.dokkcorp.dashboard.features.crypto.hype;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dokkcorp.dashboard.providers.CoinGeckoClient;
import com.dokkcorp.dashboard.providers.dto.CoinGeckoDto;
import com.dokkcorp.dashboard.providers.dto.CoinGeckoHistoryDto;

import com.dokkcorp.dashboard.providers.HyperliquidClient;
import com.dokkcorp.dashboard.providers.dto.HyperliquidDto;

import com.dokkcorp.dashboard.providers.BlockChainClient;
import com.dokkcorp.dashboard.providers.dto.BlockChainDto;

import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;

import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

@Service
public class HypeService {

        @Autowired
        private CoinGeckoClient client;

        @Autowired
        private HyperliquidClient hyperliquidClient;

        @Autowired
        private BlockChainClient blockChainClient;

        @Autowired
        private AssetDailyRepository assetDailyRepository;

        @Autowired
        private AssetSnapshotRepository assetSnapshotRepository;

        private HypeDto cachedData;

        public HypeDto getLastHypeData() {
                if (this.cachedData != null) {
                        return this.cachedData;
                }
                this.cachedData = this.assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE")
                                .map(this::mapToDto)
                                .orElseGet(this::getData);
                return this.cachedData;
        }

        public HypeDto getData() {

                HyperliquidDto hyperliquidData = this.hyperliquidClient.getHlData();

                CoinGeckoDto[] data = this.client.getData();
                CoinGeckoDto hypeRaw = data[0];

                AssetDaily newPoint = new AssetDaily();
                newPoint.setSymbol("HYPE");
                newPoint.setCurrentPrice(hypeRaw.currentPrice());
                newPoint.setMarketCap(hypeRaw.marketCap());
                newPoint.setPriceChangePercentage24h(hypeRaw.priceChangePercentage24h());
                newPoint.setTotalVolume(hypeRaw.totalVolume());
                newPoint.setLastRefresh(System.currentTimeMillis());
                newPoint.setBurnedHype(hyperliquidData.hypeBurned());
                newPoint.setCirculatingSupply(hyperliquidData.circulatingSupply());
                newPoint.setFeesDaily(String.valueOf(Double.parseDouble(hyperliquidData.dailyVolume()) * 0.00022));
                newPoint.setDailyVolume(hyperliquidData.dailyVolume());
                newPoint.setOpenInterest(hyperliquidData.openInterest());
                newPoint.setTotalValueLocked(hyperliquidData.totalValueLocked());

                if (this.assetSnapshotRepository.findFirstByOrderByDayDesc().isEmpty()) {
                        initializeHistory();
                }

                AssetDaily savedEntity = this.assetDailyRepository.save(newPoint);
                this.cachedData = mapToDto(savedEntity);
                return this.cachedData;
        }

        private void initializeHistory() {

                CoinGeckoHistoryDto history = this.client.getHistory();

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

        private HypeDto mapToDto(AssetDaily entity) {

                List<AssetSnapshot> history = new java.util.ArrayList<>(
                                this.assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("HYPE"));
                java.util.Collections.reverse(history);
                List<Double> historicalPrice = history.stream().map(AssetSnapshot::getPrice).toList();
                List<Long> historicalDays = history.stream().map(AssetSnapshot::getDay).toList();

                List<AssetDaily> daily = new java.util.ArrayList<>(
                                this.assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("HYPE"));
                java.util.Collections.reverse(daily);
                List<Double> livePrice = daily.stream().map(AssetDaily::getCurrentPrice).toList();
                List<Long> liveDay = daily.stream().map(AssetDaily::getLastRefresh).toList();

                HyperliquidDto hyperliquidData = this.hyperliquidClient.getHlData();
                BlockChainDto blockchainData = this.blockChainClient.getBlockchainData();

                // Les calculs
                // Ratio Vol/TVL provider
                double volume = Double.parseDouble(hyperliquidData.dailyVolume());
                double tvl = Double.parseDouble(hyperliquidData.totalValueLocked());
                double ratioVolTvl = volume / tvl;
                String ratioProvider = Double.toString(ratioVolTvl);

                // % en circulation
                double circulation100temp = Double.parseDouble(hyperliquidData.circulatingSupply())
                                / Double.parseDouble(hyperliquidData.maxSupply()) * 100;
                String circulation100 = String.valueOf(circulation100temp);

                // Marketcap
                double marketCap = Double.parseDouble(hyperliquidData.circulatingSupply()) * entity.getCurrentPrice();

                // FDV
                double fdvtemp = Double.parseDouble(hyperliquidData.maxSupply()) * entity.getCurrentPrice();
                String fdv = String.valueOf(fdvtemp);

                // Ratio Mcap/FDV
                double ratioMcapFdvtemp = marketCap / fdvtemp;
                String ratioMcapFdv = String.valueOf(ratioMcapFdvtemp);

                // Hype brulé
                double hypeBurnedtemp = (Double.parseDouble(hyperliquidData.hypeBurned()) / 1000000000) * 100;
                String hypeBurned = String.valueOf(hypeBurnedtemp);

                // Estimation fees
                double feesDaily = volume * 0.00022;
                double averageDailyFees = history.stream()
                                .filter(snapshot -> snapshot.getFees24h() != null)
                                .mapToDouble(AssetSnapshot::getFees24h)
                                .average()
                                .orElse(feesDaily);
                double feesAnnual = averageDailyFees * 365;

                // Ratio Price to Fees
                double ratioPriceFeesTemp = marketCap / feesAnnual;
                String ratioPriceFees = String.valueOf(ratioPriceFeesTemp);

                // Ratio OI/Mcap
                double ratioOImcapTemp = Double.parseDouble(hyperliquidData.openInterest()) / marketCap;
                String ratioOImcap = String.valueOf(ratioOImcapTemp);

                // Ratio % staké
                double ratioStakedTemp = Double.parseDouble(hyperliquidData.totalStakedHype())
                                / Double.parseDouble(hyperliquidData.maxSupply()) * 100;
                String ratioStaked = String.valueOf(ratioStakedTemp);

                // % bridged hype sur EVM
                double ratioBridgedTemp = Double.parseDouble(blockchainData.bridgedHype())
                                / Double.parseDouble(hyperliquidData.circulatingSupply()) * 100;
                String ratioBridged = String.valueOf(ratioBridgedTemp);

                // % staké evm vs core
                double stakedEvmCoreTemp = (Double.parseDouble(blockchainData.liquidStaked())
                                / Double.parseDouble(hyperliquidData.circulatingSupply())) * 100;
                String stakedEvmCore = String.valueOf(stakedEvmCoreTemp);

                // Récup les données h24 (le premier élément après reverse est celui d'il y a 24h)
                AssetDaily h24 = daily.get(0);
                // burn 24h
                double oldBurned = h24.getBurnedHype() != null
                                ? Double.parseDouble(h24.getBurnedHype())
                                : Double.parseDouble(hyperliquidData.hypeBurned());
                double burned24h = Double.parseDouble(hyperliquidData.hypeBurned()) - oldBurned;
                // Issued 24h
                double oldCirculating = h24.getCirculatingSupply() != null
                                ? Double.parseDouble(h24.getCirculatingSupply())
                                : Double.parseDouble(hyperliquidData.circulatingSupply());
                double issued24h = Double.parseDouble(hyperliquidData.circulatingSupply()) - oldCirculating;

                // Les volat dernières 24h
                double volatVolume = 0;
                double volatOpenInterest = 0;
                double volatFees = 0;
                double volatHlpProvider = 0;
                if (this.assetSnapshotRepository.findFirstBySymbolOrderByDayDesc("HYPE").isEmpty()
                                || this.assetSnapshotRepository.findFirstBySymbolOrderByDayDesc("HYPE").get()
                                                .getVolume24h() == null) {

                } else {
                        try {
                                volatVolume = ((volume / Double.parseDouble(h24.getDailyVolume())) - 1) * 100;
                                volatOpenInterest = ((Double.parseDouble(hyperliquidData.openInterest())
                                                / Double.parseDouble(h24.getOpenInterest()))
                                                - 1)
                                                * 100;
                                volatFees = ((feesDaily / Double.parseDouble(h24.getFeesDaily())) - 1) * 100;
                                volatHlpProvider = ((tvl / Double.parseDouble(h24.getTotalValueLocked())) - 1) * 100;
                        } catch (Exception e) {

                        }
                }

                // data 30j
                String burned30d = "0";
                String circulating30d = "0";
                String flux30d = "0";
                try {

                        if (history.size() >= 30) {
                                double burned30dTemp = Double
                                                .parseDouble(history.get(history.size() - 30).getBurnedHype());
                                double burned30dTemp2 = ((Double.parseDouble(hyperliquidData.hypeBurned())
                                                - burned30dTemp)
                                                / 30);
                                burned30d = String.valueOf(burned30dTemp2);

                                double circulating30dTemp = Double
                                                .parseDouble(history.get(history.size() - 30).getCirculatingSupply());
                                double circulating30dTemp2 = ((Double.parseDouble(hyperliquidData.circulatingSupply())
                                                - circulating30dTemp) / 30);
                                circulating30d = String.valueOf(circulating30dTemp2);

                                double flux30dTemp = circulating30dTemp2 - burned30dTemp2;
                                flux30d = String.valueOf(flux30dTemp);

                        } else {
                                double burned30dTemp = Double.parseDouble(history.get(0).getBurnedHype());
                                double burned30dTemp2 = ((Double.parseDouble(hyperliquidData.hypeBurned())
                                                - burned30dTemp)
                                                / history.size());
                                burned30d = String.valueOf(burned30dTemp2);

                                double circulating30dTemp = Double.parseDouble(history.get(0).getCirculatingSupply());
                                double circulating30dTemp2 = ((Double.parseDouble(hyperliquidData.circulatingSupply())
                                                - circulating30dTemp) / history.size());
                                circulating30d = String.valueOf(circulating30dTemp2);

                                double flux30dTemp = circulating30dTemp2 - burned30dTemp2;
                                flux30d = String.valueOf(flux30dTemp);
                        }
                } catch (Exception e) {

                }

                return new HypeDto(
                                entity.getSymbol(),
                                entity.getCurrentPrice(),
                                marketCap,
                                entity.getPriceChangePercentage24h(),
                                entity.getTotalVolume(),
                                entity.getLastRefresh(),
                                historicalPrice,
                                historicalDays,
                                livePrice,
                                liveDay,
                                hyperliquidData.circulatingSupply(),
                                hyperliquidData.totalValueLocked(),
                                hyperliquidData.apr(),
                                hyperliquidData.dailyVolume(),
                                ratioProvider,
                                hyperliquidData.openInterest(),
                                Double.toString(feesDaily),
                                Double.toString(feesAnnual),
                                volatVolume,
                                volatOpenInterest,
                                volatFees,
                                volatHlpProvider,
                                hyperliquidData.stakingApr(),
                                hyperliquidData.maxSupply(),
                                circulation100,
                                fdv,
                                ratioMcapFdv,
                                hypeBurned,
                                ratioPriceFees,
                                ratioOImcap,
                                hyperliquidData.totalStakedHype(),
                                ratioStaked,
                                blockchainData.bridgedHype(),
                                ratioBridged,
                                blockchainData.liquidStaked(),
                                stakedEvmCore,
                                burned30d,
                                circulating30d,
                                flux30d,
                                burned24h);
        }
}
