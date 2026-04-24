package com.dokkcorp.dashboard.jobs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.dokkcorp.dashboard.features.crypto.hype.HypeService;

import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

@Component
public class AssetSyncJob {

    @Autowired
    private AssetSnapshotRepository assetSnapshotRepository;

    @Autowired
    private AssetDailyRepository assetDailyRepository;

    @Autowired
    private HypeService hypeService;

    // toutes les 10 minutes, tempo 5minutes pour les test
    @Scheduled(fixedRate = 300000)
    public void autoSync() {
        this.hypeService.getData();
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
    public void sendCurrentPriceToDB() {
        AssetDaily ad = this.assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE").get();

        AssetSnapshot s = new AssetSnapshot();
        s.setSymbol("HYPE");
        s.setPrice(ad.getCurrentPrice());
        s.setDay(ad.getLastRefresh());
        this.assetSnapshotRepository.save(s);
    }
}
