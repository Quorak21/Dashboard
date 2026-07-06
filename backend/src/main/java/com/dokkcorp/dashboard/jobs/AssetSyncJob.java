package com.dokkcorp.dashboard.jobs;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.dokkcorp.dashboard.features.crypto.hype.HypeService;
import com.dokkcorp.dashboard.features.crypto.hype.HypeDto;
import com.dokkcorp.dashboard.features.assets.AssetRegistry;
import com.dokkcorp.dashboard.features.assets.ConfigurableAssetService;
import com.dokkcorp.dashboard.features.assets.MarketHoursGuard;
import com.dokkcorp.dashboard.features.assets.ProviderCallMetrics;
import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetProvider;
import java.util.List;
import java.util.Optional;

import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;

import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AssetSyncJob {

    private static final Logger logger = LoggerFactory.getLogger(AssetSyncJob.class);

    private final AssetSnapshotRepository assetSnapshotRepository;

    private final AssetDailyRepository assetDailyRepository;

    private final HypeService hypeService;

    private final ConfigurableAssetService configurableAssetService;

    private final AssetRegistry assetRegistry;

    private final MarketHoursGuard marketHoursGuard;

    private final ProviderCallMetrics providerCallMetrics;

    public AssetSyncJob(AssetSnapshotRepository assetSnapshotRepository, AssetDailyRepository assetDailyRepository,
            HypeService hypeService, ConfigurableAssetService configurableAssetService, AssetRegistry assetRegistry,
            MarketHoursGuard marketHoursGuard, ProviderCallMetrics providerCallMetrics) {
        this.assetSnapshotRepository = assetSnapshotRepository;
        this.assetDailyRepository = assetDailyRepository;
        this.hypeService = hypeService;
        this.configurableAssetService = configurableAssetService;
        this.assetRegistry = assetRegistry;
        this.marketHoursGuard = marketHoursGuard;
        this.providerCallMetrics = providerCallMetrics;
    }

    // toutes les 10 minutes sur des chiffres ronds (00, 10, 20...)
    // Tache principale, met a jour la data pour le front et stock en BD
    @Scheduled(cron = "0 0/10 * * * ?")
    public void autoSync() {
        this.hypeService.getData();
    }

    @Scheduled(cron = "0 0/15 * * * ?")
    public void syncFmpAssets() {
        List<AssetDefinition> fmpAssets = this.assetRegistry.byProvider(AssetProvider.FMP);
        if (fmpAssets != null) {
            for (AssetDefinition asset : fmpAssets) {
                if (asset == null) {
                    continue;
                }
                try {
                    if (this.marketHoursGuard.isOpen(asset)) {
                        this.configurableAssetService.syncPrice(asset.id());
                    }
                } catch (Exception e) {
                    logger.error("Erreur lors de la synchronisation de l'actif FMP : {}", asset.id(), e);
                }
            }
        }
        this.providerCallMetrics.logMetrics();
    }

    // Tâche de mise en BD à la cloture des marchés
    @Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
    public void sendDailySnapshotToDb() {
        // Chaque symbole est indépendant : un HYPE dégradé ne doit pas
        // empêcher la sauvegarde des snapshots du registre (et inversement).
        sendHypeSnapshot();
        sendRegistrySnapshots();
    }

    private void sendRegistrySnapshots() {
        List<AssetDefinition> assets = this.assetRegistry.all();
        if (assets == null) {
            return;
        }
        for (AssetDefinition asset : assets) {
            if (asset == null) {
                continue;
            }
            try {
                saveRegistrySnapshot(asset);
            } catch (Exception e) {
                logger.error("Sauvegarde du snapshot {} ({}) n'a pas fonctionné",
                        asset.dbSymbol(), asset.id(), e);
            }
        }
    }

    private void saveRegistrySnapshot(AssetDefinition asset) {
        Optional<AssetDaily> latest = this.assetDailyRepository
                .findFirstBySymbolOrderByLastRefreshDesc(asset.dbSymbol());
        if (latest.isEmpty()) {
            logger.warn("Snapshot {} reporté : aucun point AssetDaily", asset.dbSymbol());
            return;
        }
        AssetDaily ad = latest.get();
        if (ad.getLastRefresh() == null) {
            logger.warn("Snapshot {} reporté : timestamp de dernière mise à jour manquant", asset.dbSymbol());
            return;
        }
        Double price = ad.getCurrentPrice();
        if (price == null) {
            logger.warn("Snapshot {} reporté : données dégradées (currentPrice null)", asset.dbSymbol());
            return;
        }
        boolean exists = this.assetSnapshotRepository.existsBySymbolAndDay(asset.dbSymbol(), ad.getLastRefresh());
        if (exists) {
            logger.info("Snapshot {} pour la date {} déjà existant, skip", asset.dbSymbol(), ad.getLastRefresh());
            return;
        }
        AssetSnapshot snapshot = new AssetSnapshot();
        snapshot.setSymbol(asset.dbSymbol());
        snapshot.setPrice(price);
        snapshot.setDay(ad.getLastRefresh());
        this.assetSnapshotRepository.save(snapshot);
    }

    private void sendHypeSnapshot() {
        try {
            // Récup la derniere MAJ
            AssetDaily ad = this.assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE")
                    .orElseThrow(() -> new IllegalStateException("Pas d'entrée hype dans la table Daily, WTF ?"));
            HypeDto data = this.hypeService.getData();

            // On garde les Double (nullable) : les DTO en fallback renvoient null
            // quand une API externe est dégradée. Un auto-unboxing planterait.
            Double volume24H = data.valuation().dailyVolume();
            Double fees24H = data.valuation().feesDaily();
            Double hlpProvider = data.hlp().providerTvl();
            Double openInterest = data.valuation().openInterest();

            // Données essentielles manquantes : on reporte le snapshot plutôt
            // que de perdre silencieusement la ligne du jour sur un NPE.
            if (volume24H == null || fees24H == null || hlpProvider == null || openInterest == null) {
                logger.warn("Snapshot HYPE reporté : données dégradées "
                        + "(volume24h={}, fees24h={}, hlpProvider={}, openInterest={})",
                        volume24H, fees24H, hlpProvider, openInterest);
                return;
            }
            // Création de l'objet a mettre en DB
            AssetSnapshot hypeSnapshot = new AssetSnapshot();
            hypeSnapshot.setSymbol("HYPE");
            hypeSnapshot.setPrice(ad.getCurrentPrice());
            hypeSnapshot.setDay(ad.getLastRefresh());
            hypeSnapshot.setVolume24h(volume24H);
            hypeSnapshot.setFees24h(fees24H);
            hypeSnapshot.setHlpProvider(hlpProvider);
            hypeSnapshot.setOpenInterest(openInterest);
            hypeSnapshot.setBurnedHype(ad.getBurnedHype());
            hypeSnapshot.setCirculatingSupply(ad.getCirculatingSupply());
            this.assetSnapshotRepository.save(hypeSnapshot);
        } catch (Exception e) {
            logger.error("Sauvegarde du snapshot HYPE n'a pas fonctionné", e);
        }
    }

    // Nettoyage BD, s'effectue le dimanche à minuit, tous les dimanche a minuit
    @Scheduled(cron = "0 0 0 * * SUN")
    @Transactional
    public void cleanDB() {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);

        this.assetDailyRepository.deleteByLastRefreshBefore(sevenDaysAgo);
        this.assetSnapshotRepository.deleteByDayBefore(oneYearAgo);
    }

}
