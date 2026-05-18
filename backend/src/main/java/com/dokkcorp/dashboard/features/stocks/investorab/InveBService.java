package com.dokkcorp.dashboard.features.stocks.investorab;

import org.springframework.stereotype.Service;

import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.providers.fmp.FMPClient;
import com.dokkcorp.dashboard.providers.fmp.FMPDto;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InveBService {

    private final FMPClient fmpClient;

    private final AssetSnapshotRepository assetSnapshotRepository;

    private static final Logger logger = LoggerFactory.getLogger(InveBService.class);

    public InveBService(FMPClient fmpClient, AssetSnapshotRepository assetSnapshotRepository) {
        this.fmpClient = fmpClient;
        this.assetSnapshotRepository = assetSnapshotRepository;
    }

    private InveBDto cachedData;
    private List<Double> historyPrices = new ArrayList<>();
    private List<Long> historyDays = new ArrayList<>();
    private long lastHistoryRefresh = 0;

    // Quand le front demande, on renvoie ce qu'il y a en cache. S'il est vide, on
    // va la remplir
    public InveBDto getLastInveBData() {
        if (this.cachedData != null) {
            return this.cachedData;
        }
        return this.getData();
    }

    // Récupération des données + mise en cache et mise a jour BD si tout va bien
    public InveBDto getData() {
        // On récupère l'historique en BD pour la chart yearly
        this.refreshHistory();

        try {
            FMPDto[] response = this.fmpClient.getData("INVE-B.ST");
            if (response == null || response.length == 0) {
                throw new RuntimeException("Données FMP vides");
            }
            FMPDto inveBRaw = response[0];
            String symbol = inveBRaw.symbol();

            // FMP renvoie le prix brut en SEK (car .ST)
            double currentPrice = inveBRaw.currentPrice();
            double marketCap = inveBRaw.marketCap();

            double priceChangePercentage24h = inveBRaw.priceChangePercentage24h();
            double totalVolume = inveBRaw.totalVolume();
            double lastRefresh = System.currentTimeMillis();

            this.cachedData = new InveBDto(
                    symbol,
                    currentPrice,
                    marketCap,
                    priceChangePercentage24h,
                    totalVolume,
                    lastRefresh,
                    this.historyPrices,
                    this.historyDays);

            return this.cachedData;
        } catch (ArithmeticException | NumberFormatException e) {
            logger.warn("Erreur de calcul");
            if (this.cachedData != null) {
                return this.cachedData;
            }
            return InveBDto.error("ERROR");
        } catch (Exception e) {
            logger.error("API FMP HS", e);
            if (this.cachedData != null) {
                return this.cachedData;
            }
            return InveBDto.error("ERROR");
        }

    }

    // Recharge l'historique depuis la BD
    private void refreshHistory() {
        // Si ça fait moins de 1h (éviter l'harcelement DB inutile) et que l'historique
        // est pas vide, on dégage
        if (System.currentTimeMillis() - lastHistoryRefresh < 60 * 60 * 1000 && !historyPrices.isEmpty()) {
            return;
        }

        // Envoie les données du plus récent au plus ancien des 365 derniers jours
        List<AssetSnapshot> snapshots = this.assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("INVE-B");

        // On inverse pour avoir le grpahique a l'endroit
        List<Double> prices = new ArrayList<>();
        List<Long> days = new ArrayList<>();
        for (int i = snapshots.size() - 1; i >= 0; i--) {
            AssetSnapshot snap = snapshots.get(i);
            prices.add(snap.getPrice());
            days.add(snap.getDay());
        }
        // On met a jour et remet le timer a 0
        this.historyPrices = prices;
        this.historyDays = days;
        this.lastHistoryRefresh = System.currentTimeMillis();
    }

}
