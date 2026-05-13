package com.dokkcorp.dashboard.jobs;

import org.springframework.beans.factory.annotation.Autowired;
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

@Component
public class AssetSyncJob {

    @Autowired
    private AssetSnapshotRepository assetSnapshotRepository;

    @Autowired
    private AssetDailyRepository assetDailyRepository;

    @Autowired
    private HypeService hypeService;

    @Autowired
    private InveBService inveBService;

    // toutes les 10 minutes sur des chiffres ronds (00, 10, 20...)
    @Scheduled(cron = "0 0/10 * * * ?")
    public void autoSync() {
        this.hypeService.getData();
        this.inveBService.getData();
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
    public void sendDailySnapshotToDb() {
        // --- HYPE ---
        AssetDaily ad = this.assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE").get();
        HypeDto data = this.hypeService.getData();

        double volume24H = Double.parseDouble(data.dailyVolume());
        double fees24H = Double.parseDouble(data.feesDaily());
        double hlpProvider = Double.parseDouble(data.totalValueLocked());
        double openInterest = Double.parseDouble(data.openInterest());

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

        // --- INVE-B ---
        InveBDto inveBData = this.inveBService.getLastInveBData();
        AssetSnapshot inveBSnapshot = new AssetSnapshot();
        inveBSnapshot.setSymbol("INVE-B");
        inveBSnapshot.setPrice(inveBData.currentPrice()); // Prix en USD
        inveBSnapshot.setDay(System.currentTimeMillis());
        this.assetSnapshotRepository.save(inveBSnapshot);
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
