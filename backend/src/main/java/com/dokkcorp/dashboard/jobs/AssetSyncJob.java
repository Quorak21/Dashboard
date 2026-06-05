package com.dokkcorp.dashboard.jobs;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.dokkcorp.dashboard.features.crypto.hype.HypeService;
import com.dokkcorp.dashboard.features.crypto.hype.HypeDto;
import com.dokkcorp.dashboard.features.stocks.investorab.InveBService;
import com.dokkcorp.dashboard.features.stocks.investorab.InveBDto;

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

    private final InveBService inveBService;

    public AssetSyncJob(AssetSnapshotRepository assetSnapshotRepository, AssetDailyRepository assetDailyRepository,
            HypeService hypeService, InveBService inveBService) {
        this.assetSnapshotRepository = assetSnapshotRepository;
        this.assetDailyRepository = assetDailyRepository;
        this.hypeService = hypeService;
        this.inveBService = inveBService;
    }

    // toutes les 10 minutes sur des chiffres ronds (00, 10, 20...)
    // Tache principale, met a jour la data pour le front et stock en BD
    @Scheduled(cron = "0 0/10 * * * ?")
    public void autoSync() {
        this.hypeService.getData();
        this.inveBService.getData();
    }

    // Tâche de mise en BD à la cloture des marchés
    @Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
    public void sendDailySnapshotToDb() {
        // Chaque symbole est indépendant : un HYPE dégradé ne doit pas
        // empêcher la sauvegarde du snapshot INVE-B (et inversement).
        sendHypeSnapshot();
        sendInveBSnapshot();
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

    private void sendInveBSnapshot() {
        try {
            AssetDaily ad = this.assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("INVE-B")
                    .orElseThrow(() -> new IllegalStateException("Pas d'entrée INVE-B dans la table Daily, WTF ?"));
            InveBDto inveBData = this.inveBService.getLastInveBData();

            // currentPrice est nullable (DTO en fallback) : on ne sauve pas
            // un snapshot avec un prix null.
            Double price = inveBData.currentPrice();
            if (price == null) {
                logger.warn("Snapshot INVE-B reporté : données dégradées (currentPrice null)");
                return;
            }

            AssetSnapshot inveBSnapshot = new AssetSnapshot();
            inveBSnapshot.setSymbol("INVE-B");
            inveBSnapshot.setPrice(price); // Prix en USD
            inveBSnapshot.setDay(ad.getLastRefresh());
            this.assetSnapshotRepository.save(inveBSnapshot);
        } catch (Exception e) {
            logger.error("Sauvegarde du snapshot INVESTOR AB n'a pas fonctionné", e);
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
