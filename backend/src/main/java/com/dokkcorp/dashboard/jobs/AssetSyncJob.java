package com.dokkcorp.dashboard.jobs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.dokkcorp.dashboard.features.crypto.hype.HypeService;
import com.dokkcorp.dashboard.features.crypto.hype.HypeDto;

import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

import jakarta.transaction.Transactional;

@Component
public class AssetSyncJob {

    @Autowired
    private AssetSnapshotRepository assetSnapshotRepository;

    @Autowired
    private AssetDailyRepository assetDailyRepository;

    @Autowired
    private HypeService hypeService;

    // toutes les 10 minutes
    @Scheduled(fixedRate = 600000)
    public void autoSync() {
        this.hypeService.getData();
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
    public void sendDailySnapshotToDb() {
        AssetDaily ad = this.assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE").get();
        HypeDto data = this.hypeService.getData();

        double volume24H = Double.parseDouble(data.dailyVolume());
        double fees24H = Double.parseDouble(data.feesDaily());
        double hlpProvider = Double.parseDouble(data.totalValueLocked());
        double openInterest = Double.parseDouble(data.openInterest());

        AssetSnapshot s = new AssetSnapshot();
        s.setSymbol("HYPE");
        s.setPrice(ad.getCurrentPrice());
        s.setDay(ad.getLastRefresh());
        s.setVolume24h(volume24H);
        s.setFees24h(fees24H);
        s.setHlpProvider(hlpProvider);
        s.setOpenInterest(openInterest);
        s.setBurnedHype(ad.getBurnedHype());
        s.setCirculatingSupply(ad.getCirculatingSupply());
        this.assetSnapshotRepository.save(s);
    }

    @Scheduled(cron = "0 0 0 * * SUN")
    @Transactional
    public void cleanDB() {
        long sevenDayAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        long oneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000);

        this.assetDailyRepository.deleteByLastRefreshBefore(sevenDayAgo);
        this.assetSnapshotRepository.deleteByDayBefore(oneYearAgo);
    }

}
