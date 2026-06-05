package com.dokkcorp.dashboard.features.crypto.hype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dokkcorp.dashboard.features.crypto.hype.maths.HypeCalculator;
import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.providers.blockchain.BlockChainClient;
import com.dokkcorp.dashboard.providers.blockchain.BlockChainDto;
import com.dokkcorp.dashboard.providers.coingecko.CoinGeckoClient;
import com.dokkcorp.dashboard.providers.coingecko.CoinGeckoDto;
import com.dokkcorp.dashboard.providers.hyperliquid.HyperliquidClient;
import com.dokkcorp.dashboard.providers.hyperliquid.HyperliquidDto;
import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

class HypeServiceTest {

    private final CoinGeckoClient coinGeckoClient = mock(CoinGeckoClient.class);
    private final HyperliquidClient hyperliquidClient = mock(HyperliquidClient.class);
    private final BlockChainClient blockChainClient = mock(BlockChainClient.class);
    private final AssetDailyRepository assetDailyRepository = mock(AssetDailyRepository.class);
    private final AssetSnapshotRepository assetSnapshotRepository = mock(AssetSnapshotRepository.class);
    private final HypeCalculator hypeCalculator = new HypeCalculator();
    private final HypeMapper hypeMapper = new HypeMapper(assetDailyRepository, assetSnapshotRepository, hypeCalculator);

    private final HypeService service = new HypeService(
            coinGeckoClient,
            hyperliquidClient,
            blockChainClient,
            assetDailyRepository,
            assetSnapshotRepository,
            hypeMapper);

    @Test
    void getLastHypeData_returnsCachedValueWithoutExternalCalls() {
        stubHappyPathProviders();
        stubEmptyDb();
        stubHistoryInitializationCheck();
        when(assetDailyRepository.save(any(AssetDaily.class))).thenReturn(buildSavedDaily());

        service.getData();
        HypeDto cachedResult = service.getLastHypeData();

        assertEquals(2d, cachedResult.summary().currentPrice());
        verify(hyperliquidClient, times(1)).getHlData();
        verify(blockChainClient, times(1)).getBlockchainData();
        verify(coinGeckoClient, times(1)).getData();
    }

    @Test
    void getData_returnsErrorWhenProvidersFailAndNoCacheExists() {
        stubEmptyDb();
        when(hyperliquidClient.getHlData()).thenThrow(new RuntimeException("rate limited"));
        when(blockChainClient.getBlockchainData()).thenThrow(new RuntimeException("down"));
        when(coinGeckoClient.getData()).thenThrow(new RuntimeException("down"));

        HypeDto result = service.getData();

        assertEquals("HYPE", result.summary().symbol());
        assertNull(result.summary().currentPrice());
        verify(assetDailyRepository, never()).save(any(AssetDaily.class));
    }

    @Test
    void getData_keepsCachedThemesWhenHyperliquidFailsAfterFirstSuccess() {
        stubHappyPathProviders();
        stubEmptyDb();
        stubHistoryInitializationCheck();
        when(assetDailyRepository.save(any(AssetDaily.class))).thenReturn(buildSavedDaily());

        HypeDto first = service.getData();
        assertNotNull(first.supply().circulatingSupply());

        when(hyperliquidClient.getHlData()).thenThrow(new RuntimeException("failed"));

        HypeDto second = service.getData();

        assertEquals(first.supply().circulatingSupply(), second.supply().circulatingSupply());
        assertEquals(2d, second.summary().currentPrice());
        verify(assetDailyRepository, times(1)).save(any(AssetDaily.class));
    }

    @Test
    void getData_blockchainDown_keepsSupplyFreshWhenHlAndCgOk() {
        stubHappyPathProviders();
        when(blockChainClient.getBlockchainData()).thenThrow(new RuntimeException("rpc down"));
        stubEmptyDb();
        stubHistoryInitializationCheck();
        when(assetDailyRepository.save(any(AssetDaily.class))).thenReturn(buildSavedDaily());

        HypeDto result = service.getData();

        assertEquals(1000d, result.supply().circulatingSupply());
        assertNull(result.blockchain().bridgedHype());
        verify(blockChainClient, times(1)).getBlockchainData();
        verify(assetDailyRepository).save(any(AssetDaily.class));
    }

    @Test
    void getData_coinGeckoDown_skipsDbSaveAndKeepsSupplyFresh() {
        when(hyperliquidClient.getHlData()).thenReturn(hyperliquidDto());
        when(blockChainClient.getBlockchainData()).thenReturn(new BlockChainDto("100", "50"));
        when(coinGeckoClient.getData()).thenThrow(new RuntimeException("rate limited"));
        stubEmptyDb();
        stubHistoryInitializationCheck();

        HypeDto result = service.getData();

        assertEquals(1000d, result.supply().circulatingSupply());
        assertNull(result.summary().currentPrice());
        verify(assetDailyRepository, never()).save(any(AssetDaily.class));
    }

    @Test
    void getData_returnsMixedCacheWhenAllProvidersFailAfterPartialCache() {
        stubHappyPathProviders();
        stubEmptyDb();
        stubHistoryInitializationCheck();
        when(assetDailyRepository.save(any(AssetDaily.class))).thenReturn(buildSavedDaily());

        HypeDto cached = service.getData();

        when(hyperliquidClient.getHlData()).thenThrow(new RuntimeException("down"));
        when(blockChainClient.getBlockchainData()).thenThrow(new RuntimeException("down"));
        when(coinGeckoClient.getData()).thenThrow(new RuntimeException("down"));

        HypeDto result = service.getData();

        assertEquals(cached.supply().circulatingSupply(), result.supply().circulatingSupply());
        assertEquals(cached.summary().currentPrice(), result.summary().currentPrice());
        assertEquals(cached.blockchain().bridgedHype(), result.blockchain().bridgedHype());
    }

    private void stubHappyPathProviders() {
        when(hyperliquidClient.getHlData()).thenReturn(hyperliquidDto());
        when(blockChainClient.getBlockchainData()).thenReturn(new BlockChainDto("100", "50"));
        when(coinGeckoClient.getData()).thenReturn(new CoinGeckoDto[] {
                new CoinGeckoDto("hype", 2d, 0d, 3d, 4d)
        });
    }

    private HyperliquidDto hyperliquidDto() {
        return new HyperliquidDto(
                "1000",
                "100",
                "0.1",
                "200",
                "300",
                "0.2",
                "2000",
                "10",
                "500");
    }

    private void stubEmptyDb() {
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("HYPE"))
                .thenReturn(Collections.emptyList());
        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("HYPE"))
                .thenReturn(Collections.emptyList());
    }

    private void stubHistoryInitializationCheck() {
        AssetSnapshot snapshot = new AssetSnapshot();
        snapshot.setDay(java.time.Instant.ofEpochMilli(1L));
        snapshot.setPrice(1d);
        snapshot.setFees24h(0.1d);
        snapshot.setBurnedHype("1");
        snapshot.setCirculatingSupply("10");
        when(assetSnapshotRepository.findFirstByOrderByDayDesc()).thenReturn(Optional.of(snapshot));
    }

    private AssetDaily buildSavedDaily() {
        AssetDaily daily = new AssetDaily();
        daily.setSymbol("HYPE");
        daily.setCurrentPrice(2d);
        daily.setMarketCap(2000d);
        daily.setPriceChangePercentage24h(3d);
        daily.setTotalVolume(4d);
        daily.setLastRefresh(java.time.Instant.ofEpochMilli(123L));
        return daily;
    }

}
