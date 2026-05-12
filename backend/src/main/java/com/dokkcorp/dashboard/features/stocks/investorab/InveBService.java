package com.dokkcorp.dashboard.features.stocks.investorab;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.providers.stocks.FMPClient;
import com.dokkcorp.dashboard.providers.stocks.ForexClient;
import com.dokkcorp.dashboard.providers.dto.stocks.FMPDto;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class InveBService {

    @Autowired
    private FMPClient fmpClient;

    @Autowired
    private ForexClient forexClient;

    @Autowired
    private AssetSnapshotRepository assetSnapshotRepository;

    private InveBDto cachedData;
    private List<Double> historyPrices = new ArrayList<>();
    private List<Long> historyDays = new ArrayList<>();
    private long lastHistoryRefresh = 0;

    public InveBDto getLastInveBData() {
        if (this.cachedData != null) {
            return this.cachedData;
        }
        return this.getData();
    }

    public InveBDto getData() {

        this.refreshHistory();

        FMPDto[] response = this.fmpClient.getData("INVE-B.ST");
        if (response == null || response.length == 0) {
            System.err.println("FMP API returned empty data for Investor AB!");
            return this.cachedData != null ? this.cachedData
                    : new InveBDto("ERROR", 0, 0, 0, 0, 0, System.currentTimeMillis(), null, null);
        }

        FMPDto inveBRaw = response[0];

        String symbol = inveBRaw.symbol();
        // Conversion SEK → USD
        double ratioSekUsd = this.forexClient.getSekUsdratio();
        double currentPrice = inveBRaw.currentPrice() * ratioSekUsd;
        double marketCap = inveBRaw.marketCap() * ratioSekUsd;
        // Conversion SEK → CHF
        double ratioSekChf = this.forexClient.getSekChfRatio();
        double currentPriceChf = inveBRaw.currentPrice() * ratioSekChf;

        double priceChangePercentage24h = inveBRaw.priceChangePercentage24h();
        double totalVolume = inveBRaw.totalVolume();
        double lastRefresh = System.currentTimeMillis();

        this.cachedData = new InveBDto(
                symbol,
                currentPrice,
                currentPriceChf,
                marketCap,
                priceChangePercentage24h,
                totalVolume,
                lastRefresh,
                this.historyPrices,
                this.historyDays);

        return this.cachedData;
    }

    /**
     * Recharge l'historique depuis la base de données (max 1 fois par jour en cache mémoire).
     * Les snapshots sont enregistrés chaque nuit à minuit UTC via AssetSyncJob.
     */
    private void refreshHistory() {
        if (System.currentTimeMillis() - lastHistoryRefresh < 24 * 60 * 60 * 1000 && !historyPrices.isEmpty()) {
            return;
        }

        // findTop365BySymbolOrderByDayDesc renvoie du plus récent au plus ancien → on inverse
        List<AssetSnapshot> snapshots = this.assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("INVE-B");

        List<Double> prices = new ArrayList<>();
        List<Long> days = new ArrayList<>();
        for (int i = snapshots.size() - 1; i >= 0; i--) {
            AssetSnapshot snap = snapshots.get(i);
            prices.add(snap.getPrice());
            days.add(snap.getDay());
        }

        this.historyPrices = prices;
        this.historyDays = days;
        this.lastHistoryRefresh = System.currentTimeMillis();
    }

}
