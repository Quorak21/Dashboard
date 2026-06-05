package com.dokkcorp.dashboard.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dokkcorp.dashboard.features.crypto.hype.HypeDto;
import com.dokkcorp.dashboard.features.crypto.hype.HypeService;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeBlockchainDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeChartsDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeHlpDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSummaryDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSupplyDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeTimedDataDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeValuationDto;
import com.dokkcorp.dashboard.features.stocks.investorab.InveBDto;
import com.dokkcorp.dashboard.features.stocks.investorab.InveBService;
import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

class AssetSyncJobTest {

    private final AssetSnapshotRepository assetSnapshotRepository = mock(AssetSnapshotRepository.class);
    private final AssetDailyRepository assetDailyRepository = mock(AssetDailyRepository.class);
    private final HypeService hypeService = mock(HypeService.class);
    private final InveBService inveBService = mock(InveBService.class);
    private final AssetSyncJob job = new AssetSyncJob(assetSnapshotRepository, assetDailyRepository, hypeService, inveBService);

    @Test
    void sendDailySnapshotToDb_savesHypeAndInvebSnapshots() {
        AssetDaily hypeDaily = new AssetDaily();
        hypeDaily.setCurrentPrice(1.25d);
        hypeDaily.setLastRefresh(Instant.ofEpochMilli(1711111111L));
        hypeDaily.setBurnedHype("10");
        hypeDaily.setCirculatingSupply("100");
        AssetDaily invebDaily = new AssetDaily();
        invebDaily.setLastRefresh(Instant.ofEpochMilli(1712222222L));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE")).thenReturn(Optional.of(hypeDaily));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("INVE-B")).thenReturn(Optional.of(invebDaily));
        when(hypeService.getData()).thenReturn(buildHypeDto());
        when(inveBService.getLastInveBData()).thenReturn(new InveBDto(
                "INVE-B", 250d, 1d, 2d, 3d, 1711112222L, List.of(), List.of(), List.of(), List.of()));

        job.sendDailySnapshotToDb();

        verify(assetSnapshotRepository, times(2)).save(any(AssetSnapshot.class));
    }

    @Test
    void cleanDb_usesExpectedRetentionWindows() {
        job.cleanDB();

        verify(assetDailyRepository).deleteByLastRefreshBefore(any(Instant.class));
        verify(assetSnapshotRepository).deleteByDayBefore(any(Instant.class));
    }

    @Test
    void sendDailySnapshotToDb_usesDailyTimestampForHypeSnapshot() {
        AssetDaily hypeDaily = new AssetDaily();
        hypeDaily.setCurrentPrice(1.25d);
        hypeDaily.setLastRefresh(Instant.ofEpochMilli(1711111111L));
        hypeDaily.setBurnedHype("10");
        hypeDaily.setCirculatingSupply("100");
        AssetDaily invebDaily = new AssetDaily();
        invebDaily.setLastRefresh(Instant.ofEpochMilli(1712222222L));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE")).thenReturn(Optional.of(hypeDaily));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("INVE-B")).thenReturn(Optional.of(invebDaily));
        when(hypeService.getData()).thenReturn(buildHypeDto());
        when(inveBService.getLastInveBData()).thenReturn(new InveBDto(
                "INVE-B", 250d, 1d, 2d, 3d, 1711112222L, List.of(), List.of(), List.of(), List.of()));

        job.sendDailySnapshotToDb();

        org.mockito.ArgumentCaptor<AssetSnapshot> captor = org.mockito.ArgumentCaptor.forClass(AssetSnapshot.class);
        verify(assetSnapshotRepository, times(2)).save(captor.capture());
        AssetSnapshot hypeSnapshot = captor.getAllValues().get(0);
        assertEquals(Instant.ofEpochMilli(1711111111L), hypeSnapshot.getDay());
        assertEquals("HYPE", hypeSnapshot.getSymbol());
        AssetSnapshot invebSnapshot = captor.getAllValues().get(1);
        assertEquals(Instant.ofEpochMilli(1712222222L), invebSnapshot.getDay());
        assertEquals("INVE-B", invebSnapshot.getSymbol());
    }

    private HypeDto buildHypeDto() {
        return new HypeDto(
                new HypeSummaryDto("HYPE", 1d, 2d, 3d, 4d, 5L),
                new HypeChartsDto(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()),
                new HypeTimedDataDto(1d, 2d, 3d, 4d, 5d, 6d, 7d, List.of(), List.of(), List.of(), List.of()),
                new HypeSupplyDto(1d, 2d, 3d, 4d),
                new HypeBlockchainDto(1d, 2d, 3d, 4d),
                new HypeHlpDto(42d, 2d, 3d),
                new HypeValuationDto(1d, 2d, 3d, 120d, 75d, 4d, 5d, 6d, 7d, 8d, 9d));
    }
}
