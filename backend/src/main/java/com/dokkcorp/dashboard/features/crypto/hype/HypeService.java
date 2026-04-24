package com.dokkcorp.dashboard.features.crypto.hype;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dokkcorp.dashboard.providers.CoinGeckoClient;
import com.dokkcorp.dashboard.providers.dto.CoinGeckoDto;
import com.dokkcorp.dashboard.providers.dto.CoinGeckoHistoryDto;

import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;

import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

@Service
public class HypeService {

    @Autowired
    private CoinGeckoClient client;

    @Autowired
    private AssetDailyRepository assetDailyRepository;

    @Autowired
    private AssetSnapshotRepository assetSnapshotRepository;

    public HypeDto getLastHypeData() {
        return this.assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE")
                .map(this::mapToDto)
                .orElseGet(this::getData);
    }

    public HypeDto getData() {

        CoinGeckoDto[] data = this.client.getData();
        CoinGeckoDto hypeRaw = data[0];

        AssetDaily newPoint = new AssetDaily();
        newPoint.setSymbol("HYPE");
        newPoint.setCurrentPrice(hypeRaw.currentPrice());
        newPoint.setMarketCap(hypeRaw.marketCap());
        newPoint.setPriceChangePercentage24h(hypeRaw.priceChangePercentage24h());
        newPoint.setTotalVolume(hypeRaw.totalVolume());
        newPoint.setLastRefresh(System.currentTimeMillis());

        if (this.assetSnapshotRepository.findFirstByOrderByDayDesc().isEmpty()) {
            initializeHistory();
        }

        AssetDaily savedEntity = this.assetDailyRepository.save(newPoint);
        return mapToDto(savedEntity);
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

        List<AssetSnapshot> history = this.assetSnapshotRepository.findTop365BySymbolOrderByDayAsc("HYPE");
        List<Double> historicalPrice = history.stream().map(AssetSnapshot::getPrice).toList();
        List<Long> historicalDays = history.stream().map(AssetSnapshot::getDay).toList();

        List<AssetDaily> daily = this.assetDailyRepository.findTop288BySymbolOrderByLastRefreshAsc("HYPE");
        List<Double> livePrice = daily.stream().map(AssetDaily::getCurrentPrice).toList();
        List<Long> liveDay = daily.stream().map(AssetDaily::getLastRefresh).toList();

        return new HypeDto(
                entity.getSymbol(),
                entity.getCurrentPrice(),
                entity.getMarketCap(),
                entity.getPriceChangePercentage24h(),
                entity.getTotalVolume(),
                entity.getLastRefresh(),
                historicalPrice,
                historicalDays,
                livePrice,
                liveDay);
    }
}
