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
        // HYPE
        try {
            // Récup la derniere MAJ
            // TODO: Prend celle de 23:50 forcement, petit decalage de 10min, a voir
            AssetDaily ad = this.assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE")
                    .orElseThrow(() -> new IllegalStateException("Pas d'entrée hype dans la table Daily, WTF ?"));
            HypeDto data = this.hypeService.getData();

            double volume24H = Double.parseDouble(data.dailyVolume());
            double fees24H = Double.parseDouble(data.feesDaily());
            double hlpProvider = Double.parseDouble(data.totalValueLocked());
            double openInterest = Double.parseDouble(data.openInterest());
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
        // INVE-B
        try {
            InveBDto inveBData = this.inveBService.getLastInveBData();
            AssetSnapshot inveBSnapshot = new AssetSnapshot();
            inveBSnapshot.setSymbol("INVE-B");
            inveBSnapshot.setPrice(inveBData.currentPrice()); // Prix en USD
            inveBSnapshot.setDay(System.currentTimeMillis());
            this.assetSnapshotRepository.save(inveBSnapshot);
        } catch (Exception e) {
            logger.error("Sauvegarde du snapshot INVESTOR AB n'a pas fonctionné", e);
        }
    }

    // Nettoyage BD, s'effectue le dimanche à minuit, tous les dimanche a minuit
    @Scheduled(cron = "0 0 0 * * SUN")
    @Transactional
    public void cleanDB() {
        // le "L" sert a dire a Java que 1000 est un Long et non pas un Int pour pas
        // overflow
        long sevenDayAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        long oneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000);

        this.assetDailyRepository.deleteByLastRefreshBefore(sevenDayAgo); // Daily, toutes les datas > 7jours
        this.assetSnapshotRepository.deleteByDayBefore(oneYearAgo); // Snapshot, toutes les datas > 1an
    }

}
