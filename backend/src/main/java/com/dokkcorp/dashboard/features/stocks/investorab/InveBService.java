package com.dokkcorp.dashboard.features.stocks.investorab;

import org.springframework.stereotype.Service;

import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.providers.fmp.FMPClient;
import com.dokkcorp.dashboard.providers.fmp.FMPDto;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.repository.AssetDailyRepository;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

@Service
public class InveBService {

    private final FMPClient fmpClient;

    private final AssetSnapshotRepository assetSnapshotRepository;

    private final AssetDailyRepository assetDailyRepository;

    private static final Logger logger = LoggerFactory.getLogger(InveBService.class);

    public InveBService(FMPClient fmpClient, AssetSnapshotRepository assetSnapshotRepository, AssetDailyRepository assetDailyRepository) {
        this.fmpClient = fmpClient;
        this.assetSnapshotRepository = assetSnapshotRepository;
        this.assetDailyRepository = assetDailyRepository;
    }

    // volatile pour les listes pour aller uniquement dans la RAM chercher les données fraîches
    private final AtomicReference<InveBDto> cachedData = new AtomicReference<>();
    private volatile List<Double> historyPrices = new ArrayList<>();
    private volatile List<Long> historyDays = new ArrayList<>();
    private volatile long lastHistoryRefresh = 0;

    // Quand le front demande, on renvoie ce qu'il y a en cache. S'il est vide, on
    // va la remplir
    public InveBDto getLastInveBData() {
        InveBDto data = this.cachedData.get();
        if (data != null) {
            return data;
        }
        return this.getData();
    }

    // Récupération des données + mise en cache et mise a jour BD si tout va bien
    public InveBDto getData() {
        // On récupère l'historique en BD pour la chart yearly
        this.refreshHistory();

        try {
            FMPDto inveBRaw = this.fmpClient.getData("INVE-B.ST")[0];
            String symbol = inveBRaw.symbol();

            // FMP renvoie le prix brut en SEK (car .ST)
            double currentPrice = inveBRaw.currentPrice();
            double marketCap = inveBRaw.marketCap();

            double priceChangePercentage24h = inveBRaw.priceChangePercentage24h();
            double totalVolume = inveBRaw.totalVolume();
            Instant lastRefresh = Instant.now();

            // We check Stockholm Stock Exchange market hours
            ZoneId stockholmZone = ZoneId.of("Europe/Stockholm");
            ZonedDateTime nowStockholm = ZonedDateTime.now(stockholmZone);
            DayOfWeek day = nowStockholm.getDayOfWeek();
            LocalTime time = nowStockholm.toLocalTime();

            boolean isMarketHours = (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY)
                    && !time.isBefore(LocalTime.of(9, 0))
                    && !time.isAfter(LocalTime.of(17, 35));

            if (isMarketHours) {
                AssetDaily newPoint = new AssetDaily();
                newPoint.setSymbol("INVE-B");
                newPoint.setCurrentPrice(currentPrice);
                newPoint.setMarketCap(marketCap);
                newPoint.setPriceChangePercentage24h(priceChangePercentage24h);
                newPoint.setTotalVolume(totalVolume);
                newPoint.setLastRefresh(lastRefresh);
                this.assetDailyRepository.save(newPoint);
            }

            // Get historical daily data and extract the latest session
            List<AssetDaily> dailyPoints = this.assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B");
            List<AssetDaily> chronological = new ArrayList<>(dailyPoints);
            Collections.reverse(chronological);

            List<Double> livePrices = new ArrayList<>();
            List<Long> liveDays = new ArrayList<>();

            if (!chronological.isEmpty()) {
                AssetDaily latestPoint = chronological.get(chronological.size() - 1);
                LocalDate latestLocalDate = latestPoint.getLastRefresh()
                        .atZone(stockholmZone)
                        .toLocalDate();

                for (AssetDaily point : chronological) {
                    LocalDate pointLocalDate = point.getLastRefresh()
                            .atZone(stockholmZone)
                            .toLocalDate();
                    if (pointLocalDate.equals(latestLocalDate)) {
                        livePrices.add(point.getCurrentPrice());
                        liveDays.add(point.getLastRefresh().toEpochMilli());
                    }
                }
            }

            InveBDto newDto = new InveBDto(
                    symbol,
                    currentPrice,
                    marketCap,
                    priceChangePercentage24h,
                    totalVolume,
                    lastRefresh.toEpochMilli(),
                    this.historyPrices,
                    this.historyDays,
                    livePrices,
                    liveDays);

            this.cachedData.set(newDto);
            return newDto;
        } catch (ArithmeticException | NumberFormatException e) {
            logger.warn("Erreur de calcul");
            InveBDto data = this.cachedData.get();
            if (data != null) {
                return data;
            }
            return InveBDto.error("ERROR");
        } catch (Exception e) {
            logger.error("API FMP HS", e);
            InveBDto data = this.cachedData.get();
            if (data != null) {
                return data;
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
            days.add(snap.getDay().toEpochMilli());
        }
        // On met a jour et remet le timer a 0
        this.historyPrices = prices;
        this.historyDays = days;
        this.lastHistoryRefresh = System.currentTimeMillis();
    }

}
