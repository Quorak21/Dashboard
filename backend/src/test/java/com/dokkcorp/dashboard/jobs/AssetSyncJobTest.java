package com.dokkcorp.dashboard.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.dokkcorp.dashboard.features.assets.AssetRegistry;
import com.dokkcorp.dashboard.features.assets.ConfigurableAssetService;
import com.dokkcorp.dashboard.features.assets.MarketHoursGuard;
import com.dokkcorp.dashboard.features.assets.ProviderCallMetrics;
import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetProvider;
import com.dokkcorp.dashboard.features.assets.model.AssetType;
import com.dokkcorp.dashboard.features.crypto.hype.HypeDto;
import com.dokkcorp.dashboard.features.crypto.hype.HypeService;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeBlockchainDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeChartsDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeHlpDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSummaryDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSupplyDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeTimedDataDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeValuationDto;
import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

class AssetSyncJobTest {

    private final AssetSnapshotRepository assetSnapshotRepository = mock(AssetSnapshotRepository.class);
    private final AssetDailyRepository assetDailyRepository = mock(AssetDailyRepository.class);
    private final HypeService hypeService = mock(HypeService.class);
    private final ConfigurableAssetService configurableAssetService = mock(ConfigurableAssetService.class);
    private final AssetRegistry assetRegistry = mock(AssetRegistry.class);
    private final MarketHoursGuard marketHoursGuard = mock(MarketHoursGuard.class);
    private final ProviderCallMetrics providerCallMetrics = mock(ProviderCallMetrics.class);
    private final AssetSyncJob job = new AssetSyncJob(
            assetSnapshotRepository,
            assetDailyRepository,
            hypeService,
            configurableAssetService,
            assetRegistry,
            marketHoursGuard,
            providerCallMetrics
    );

    @Test
    void sendDailySnapshotToDb_savesHypeAndInvebSnapshots() {
        AssetDaily hypeDaily = new AssetDaily();
        hypeDaily.setCurrentPrice(1.25d);
        hypeDaily.setLastRefresh(Instant.ofEpochMilli(1711111111L));
        hypeDaily.setBurnedHype("10");
        hypeDaily.setCirculatingSupply("100");
        AssetDaily invebDaily = new AssetDaily();
        invebDaily.setCurrentPrice(250d);
        invebDaily.setLastRefresh(Instant.ofEpochMilli(1712222222L));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE")).thenReturn(Optional.of(hypeDaily));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("INVE-B")).thenReturn(Optional.of(invebDaily));
        when(hypeService.getData()).thenReturn(buildHypeDto());
        when(assetRegistry.all()).thenReturn(List.of(invebDefinition()));

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
        invebDaily.setCurrentPrice(250d);
        invebDaily.setLastRefresh(Instant.ofEpochMilli(1712222222L));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE")).thenReturn(Optional.of(hypeDaily));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("INVE-B")).thenReturn(Optional.of(invebDaily));
        when(hypeService.getData()).thenReturn(buildHypeDto());
        when(assetRegistry.all()).thenReturn(List.of(invebDefinition()));

        job.sendDailySnapshotToDb();

        ArgumentCaptor<AssetSnapshot> captor = ArgumentCaptor.forClass(AssetSnapshot.class);
        verify(assetSnapshotRepository, times(2)).save(captor.capture());
        AssetSnapshot hypeSnapshot = captor.getAllValues().get(0);
        assertEquals(Instant.ofEpochMilli(1711111111L), hypeSnapshot.getDay());
        assertEquals("HYPE", hypeSnapshot.getSymbol());
        AssetSnapshot invebSnapshot = captor.getAllValues().get(1);
        assertEquals(Instant.ofEpochMilli(1712222222L), invebSnapshot.getDay());
        assertEquals("INVE-B", invebSnapshot.getSymbol());
        assertEquals(250d, invebSnapshot.getPrice());
        assertNull(invebSnapshot.getVolume24h());
    }

    @Test
    void sendRegistrySnapshots_skipsNullPrice() {
        AssetDaily hypeDaily = new AssetDaily();
        hypeDaily.setCurrentPrice(1.25d);
        hypeDaily.setLastRefresh(Instant.ofEpochMilli(1711111111L));
        hypeDaily.setBurnedHype("10");
        hypeDaily.setCirculatingSupply("100");
        AssetDaily invebDaily = new AssetDaily();
        invebDaily.setCurrentPrice(null);
        invebDaily.setLastRefresh(Instant.ofEpochMilli(1712222222L));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE")).thenReturn(Optional.of(hypeDaily));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("INVE-B")).thenReturn(Optional.of(invebDaily));
        when(hypeService.getData()).thenReturn(buildHypeDto());
        when(assetRegistry.all()).thenReturn(List.of(invebDefinition()));

        job.sendDailySnapshotToDb();

        verify(assetSnapshotRepository, times(1)).save(any(AssetSnapshot.class));
    }

    @Test
    void sendRegistrySnapshots_skipsIfAlreadyExists() {
        AssetDaily hypeDaily = new AssetDaily();
        hypeDaily.setCurrentPrice(1.25d);
        hypeDaily.setLastRefresh(Instant.ofEpochMilli(1711111111L));
        hypeDaily.setBurnedHype("10");
        hypeDaily.setCirculatingSupply("100");
        AssetDaily invebDaily = new AssetDaily();
        invebDaily.setCurrentPrice(250d);
        invebDaily.setLastRefresh(Instant.ofEpochMilli(1712222222L));

        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE")).thenReturn(Optional.of(hypeDaily));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("INVE-B")).thenReturn(Optional.of(invebDaily));
        when(hypeService.getData()).thenReturn(buildHypeDto());
        when(assetRegistry.all()).thenReturn(List.of(invebDefinition()));

        when(assetSnapshotRepository.existsBySymbolAndDay("INVE-B", Instant.ofEpochMilli(1712222222L))).thenReturn(true);

        job.sendDailySnapshotToDb();

        verify(assetSnapshotRepository, times(1)).save(any(AssetSnapshot.class));
    }

    @Test
    void sendRegistrySnapshots_skipsNullLastRefresh() {
        AssetDaily hypeDaily = new AssetDaily();
        hypeDaily.setCurrentPrice(1.25d);
        hypeDaily.setLastRefresh(Instant.ofEpochMilli(1711111111L));
        hypeDaily.setBurnedHype("10");
        hypeDaily.setCirculatingSupply("100");
        AssetDaily invebDaily = new AssetDaily();
        invebDaily.setCurrentPrice(250d);
        invebDaily.setLastRefresh(null);

        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE")).thenReturn(Optional.of(hypeDaily));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("INVE-B")).thenReturn(Optional.of(invebDaily));
        when(hypeService.getData()).thenReturn(buildHypeDto());
        when(assetRegistry.all()).thenReturn(List.of(invebDefinition()));

        job.sendDailySnapshotToDb();

        verify(assetSnapshotRepository, times(1)).save(any(AssetSnapshot.class));
    }

    @Test
    void sendRegistrySnapshots_isolatesFailuresPerAsset() {
        AssetDefinition inveb = invebDefinition();
        AssetDefinition brwm = new AssetDefinition(
                "brwm", "World Mining", AssetProvider.FMP,
                "BRWM.L", "BRWM", AssetType.TRUST, "GBP", null, null, null);
        AssetDaily invebDaily = new AssetDaily();
        invebDaily.setCurrentPrice(250d);
        invebDaily.setLastRefresh(Instant.ofEpochMilli(1712222222L));
        AssetDaily brwmDaily = new AssetDaily();
        brwmDaily.setCurrentPrice(500d);
        brwmDaily.setLastRefresh(Instant.ofEpochMilli(1713333333L));

        when(assetRegistry.all()).thenReturn(List.of(inveb, brwm));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Optional.of(invebDaily));
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("BRWM"))
                .thenReturn(Optional.of(brwmDaily));
        when(assetSnapshotRepository.save(any(AssetSnapshot.class)))
                .thenThrow(new RuntimeException("DB failure"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        job.sendDailySnapshotToDb();

        verify(assetSnapshotRepository, times(2)).save(any(AssetSnapshot.class));

        ArgumentCaptor<AssetSnapshot> captor = ArgumentCaptor.forClass(AssetSnapshot.class);
        verify(assetSnapshotRepository, times(2)).save(captor.capture());
        assertEquals("INVE-B", captor.getAllValues().get(0).getSymbol());
        assertEquals("BRWM", captor.getAllValues().get(1).getSymbol());
    }

    @Test
    void sendRegistrySnapshots_handlesEmptyRegistry() {
        AssetDaily hypeDaily = new AssetDaily();
        hypeDaily.setCurrentPrice(1.25d);
        hypeDaily.setLastRefresh(Instant.ofEpochMilli(1711111111L));
        hypeDaily.setBurnedHype("10");
        hypeDaily.setCirculatingSupply("100");
        when(assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc("HYPE")).thenReturn(Optional.of(hypeDaily));
        when(hypeService.getData()).thenReturn(buildHypeDto());
        when(assetRegistry.all()).thenReturn(Collections.emptyList());

        job.sendDailySnapshotToDb();

        verify(assetSnapshotRepository, times(1)).save(any(AssetSnapshot.class));
    }

    @Test
    void syncFmpAssets_syncsOpenMarketsAndSkipsClosedMarkets() {
        AssetDefinition openAsset = new AssetDefinition("inveb", null, null, null, null, null, null, null, null, null);
        AssetDefinition closedAsset = new AssetDefinition("brwm", null, null, null, null, null, null, null, null, null);

        when(assetRegistry.byProvider(AssetProvider.FMP)).thenReturn(List.of(openAsset, closedAsset));
        when(marketHoursGuard.isOpen(openAsset)).thenReturn(true);
        when(marketHoursGuard.isOpen(closedAsset)).thenReturn(false);

        job.syncFmpAssets();

        verify(configurableAssetService, times(1)).syncPrice("inveb");
        verify(configurableAssetService, times(0)).syncPrice("brwm");
        verify(providerCallMetrics, times(1)).logMetrics();
    }

    @Test
    void syncFmpAssets_isolatesFailuresPerAsset() {
        AssetDefinition asset1 = new AssetDefinition("inveb", null, null, null, null, null, null, null, null, null);
        AssetDefinition asset2 = new AssetDefinition("brwm", null, null, null, null, null, null, null, null, null);

        when(assetRegistry.byProvider(AssetProvider.FMP)).thenReturn(List.of(asset1, asset2));
        when(marketHoursGuard.isOpen(asset1)).thenReturn(true);
        when(marketHoursGuard.isOpen(asset2)).thenReturn(true);

        when(configurableAssetService.syncPrice("inveb")).thenThrow(new RuntimeException("API failure"));

        job.syncFmpAssets();

        verify(configurableAssetService, times(1)).syncPrice("inveb");
        verify(configurableAssetService, times(1)).syncPrice("brwm");
        verify(providerCallMetrics, times(1)).logMetrics();
    }

    @Test
    void syncFmpAssets_handlesNullAssetsList() {
        when(assetRegistry.byProvider(AssetProvider.FMP)).thenReturn(null);

        job.syncFmpAssets();

        verify(configurableAssetService, times(0)).syncPrice(any());
        verify(providerCallMetrics, times(1)).logMetrics();
    }

    @Test
    void syncFmpAssets_handlesNullAssetInList() {
        AssetDefinition openAsset = new AssetDefinition("inveb", null, null, null, null, null, null, null, null, null);

        List<AssetDefinition> list = new java.util.ArrayList<>();
        list.add(null);
        list.add(openAsset);

        when(assetRegistry.byProvider(AssetProvider.FMP)).thenReturn(list);
        when(marketHoursGuard.isOpen(openAsset)).thenReturn(true);

        job.syncFmpAssets();

        verify(configurableAssetService, times(1)).syncPrice("inveb");
        verify(providerCallMetrics, times(1)).logMetrics();
    }

    private static AssetDefinition invebDefinition() {
        return new AssetDefinition(
                "inveb", "Investor AB", AssetProvider.FMP,
                "INVE-B.ST", "INVE-B", AssetType.STOCK, "SEK", null, null, null);
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
