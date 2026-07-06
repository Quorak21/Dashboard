package com.dokkcorp.dashboard.jobs;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.dokkcorp.dashboard.features.crypto.hype.HypeService;
import com.dokkcorp.dashboard.features.crypto.hype.HypeDto;
import com.dokkcorp.dashboard.features.assets.AssetRegistry;
import com.dokkcorp.dashboard.features.assets.ConfigurableAssetService;
import com.dokkcorp.dashboard.features.assets.ProviderCallMetrics;
import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetProvider;
import com.dokkcorp.dashboard.features.assets.model.SyncConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;

import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled asset synchronization.
 *
 * <p>FMP price fetches run on {@code sync.interval-minutes} regardless of market hours so the
 * dashboard cache stays warm overnight and across time zones. Daily {@code AssetDaily} persistence
 * remains gated by {@link com.dokkcorp.dashboard.features.assets.MarketHoursGuard#isOpen} inside
 * {@link ConfigurableAssetService#syncPrice(String)}.
 */
@Component
public class AssetSyncJob {

    private static final Logger logger = LoggerFactory.getLogger(AssetSyncJob.class);
    private static final long FMP_THROTTLE_MS = 500L;

    private final AssetSnapshotRepository assetSnapshotRepository;

    private final AssetDailyRepository assetDailyRepository;

    private final HypeService hypeService;

    private final ConfigurableAssetService configurableAssetService;

    private final AssetRegistry assetRegistry;

    private final ProviderCallMetrics providerCallMetrics;

    private final TaskScheduler taskScheduler;

    private final AtomicBoolean fmpSyncRunning = new AtomicBoolean(false);

    private volatile int lastWarmUpUtcMinute = -1;

    public AssetSyncJob(AssetSnapshotRepository assetSnapshotRepository, AssetDailyRepository assetDailyRepository,
            HypeService hypeService, ConfigurableAssetService configurableAssetService, AssetRegistry assetRegistry,
            ProviderCallMetrics providerCallMetrics, TaskScheduler taskScheduler) {
        this.assetSnapshotRepository = assetSnapshotRepository;
        this.assetDailyRepository = assetDailyRepository;
        this.hypeService = hypeService;
        this.configurableAssetService = configurableAssetService;
        this.assetRegistry = assetRegistry;
        this.providerCallMetrics = providerCallMetrics;
        this.taskScheduler = taskScheduler;
    }

    // toutes les 10 minutes sur des chiffres ronds (00, 10, 20...)
    // Tache principale, met a jour la data pour le front et stock en BD
    @Scheduled(cron = "0 0/10 * * * ?")
    public void autoSync() {
        this.hypeService.getData();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpFmpAssetsOnStartup() {
        taskScheduler.schedule(this::runWarmUp, Instant.now());
    }

    private void runWarmUp() {
        if (!fmpSyncRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            lastWarmUpUtcMinute = ZonedDateTime.now(ZoneOffset.UTC).getMinute();
            enqueueFmpSync(true);
        } finally {
            fmpSyncRunning.set(false);
        }
    }

    @Scheduled(cron = "0 * * * * ?")
    public void syncFmpAssets() {
        if (fmpSyncRunning.get()) {
            return;
        }
        int minuteOfHour = ZonedDateTime.now(ZoneOffset.UTC).getMinute();
        if (minuteOfHour == lastWarmUpUtcMinute) {
            return;
        }
        enqueueFmpSync(false);
    }

    static boolean isDueForSync(int minuteOfHour, SyncConfig sync) {
        if (sync == null) {
            return true;
        }
        if (sync.intervalMinutes() <= 0) {
            return false;
        }
        return Math.floorMod(minuteOfHour - sync.offsetMinutes(), sync.intervalMinutes()) == 0;
    }

    private void enqueueFmpSync(boolean forceAll) {
        int minuteOfHour = ZonedDateTime.now(ZoneOffset.UTC).getMinute();
        List<AssetDefinition> due = collectDueFmpAssets(minuteOfHour, forceAll);
        if (due.isEmpty()) {
            this.providerCallMetrics.logMetrics();
            return;
        }
        scheduleFmpSyncChain(due, 0);
    }

    private List<AssetDefinition> collectDueFmpAssets(int minuteOfHour, boolean forceAll) {
        List<AssetDefinition> fmpAssets = this.assetRegistry.byProvider(AssetProvider.FMP);
        if (fmpAssets == null) {
            return List.of();
        }
        List<AssetDefinition> due = new ArrayList<>();
        for (AssetDefinition asset : fmpAssets) {
            if (asset == null) {
                continue;
            }
            if (forceAll || isDueForSync(minuteOfHour, asset.sync())) {
                due.add(asset);
            }
        }
        return due;
    }

    private void scheduleFmpSyncChain(List<AssetDefinition> due, int index) {
        if (index >= due.size()) {
            this.providerCallMetrics.logMetrics();
            return;
        }

        AssetDefinition asset = due.get(index);
        try {
            this.configurableAssetService.syncPrice(asset.id());
        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation de l'actif FMP : {}", asset.id(), e);
        }

        int next = index + 1;
        if (next < due.size()) {
            taskScheduler.schedule(
                    () -> scheduleFmpSyncChain(due, next),
                    Instant.now().plusMillis(FMP_THROTTLE_MS));
        } else {
            taskScheduler.schedule(
                    this.providerCallMetrics::logMetrics,
                    Instant.now().plusMillis(FMP_THROTTLE_MS));
        }
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
        if (assets == null || assets.isEmpty()) {
            return;
        }
        List<String> symbols = assets.stream()
                .filter(asset -> asset != null && asset.dbSymbol() != null)
                .map(AssetDefinition::dbSymbol)
                .toList();
        if (symbols.isEmpty()) {
            return;
        }
        Map<String, AssetDaily> latestBySymbol = this.assetDailyRepository.findLatestBySymbols(symbols).stream()
                .collect(Collectors.toMap(
                        AssetDaily::getSymbol,
                        daily -> daily,
                        AssetSyncJob::preferLatestDaily));

        for (AssetDefinition asset : assets) {
            if (asset == null) {
                continue;
            }
            try {
                saveRegistrySnapshot(asset, latestBySymbol.get(asset.dbSymbol()));
            } catch (Exception e) {
                logger.error("Sauvegarde du snapshot {} ({}) n'a pas fonctionné",
                        asset.dbSymbol(), asset.id(), e);
            }
        }
    }

    static AssetDaily preferLatestDaily(AssetDaily left, AssetDaily right) {
        if (left.getCurrentPrice() != null && right.getCurrentPrice() == null) {
            return left;
        }
        if (right.getCurrentPrice() != null && left.getCurrentPrice() == null) {
            return right;
        }
        Long leftId = left.getId();
        Long rightId = right.getId();
        if (leftId != null && rightId != null) {
            return leftId > rightId ? left : right;
        }
        return left;
    }

    private void saveRegistrySnapshot(AssetDefinition asset, AssetDaily latest) {
        if (latest == null) {
            logger.warn("Snapshot {} reporté : aucun point AssetDaily", asset.dbSymbol());
            return;
        }
        AssetDaily ad = latest;
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
