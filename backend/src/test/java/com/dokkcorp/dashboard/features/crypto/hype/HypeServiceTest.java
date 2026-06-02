package com.dokkcorp.dashboard.features.crypto.hype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dokkcorp.dashboard.features.crypto.hype.maths.HypeCalculator;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeBlockchainDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeHlpDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSupplyDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeTimedDataDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeValuationDto;
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
    private final HypeCalculator hypeCalculator = mock(HypeCalculator.class);

    private final HypeService service = new HypeService(
            coinGeckoClient,
            hyperliquidClient,
            blockChainClient,
            assetDailyRepository,
            assetSnapshotRepository,
            hypeCalculator);

    @Test
    void getLastHypeData_returnsCachedValueWithoutExternalCalls() {
        stubHappyPathProviders();
        stubRepositoryReadsForMapping();
        when(assetDailyRepository.save(any(AssetDaily.class))).thenReturn(buildSavedDaily());
        stubCalculatorDtos();

        HypeDto firstResult = service.getData();
        HypeDto cachedResult = service.getLastHypeData();

        assertSame(firstResult, cachedResult);
        verify(hyperliquidClient).getHlData();
        verify(blockChainClient).getBlockchainData();
        verify(coinGeckoClient).getData();
    }

    @Test
    void getData_returnsErrorWhenProvidersFailAndNoCacheExists() {
        when(hyperliquidClient.getHlData()).thenThrow(new RuntimeException("rate limited"));
        when(blockChainClient.getBlockchainData()).thenThrow(new RuntimeException("down"));
        when(coinGeckoClient.getData()).thenThrow(new RuntimeException("down"));

        HypeDto result = service.getData();

        assertEquals("HYPE", result.summary().symbol());
        assertEquals(null, result.summary().currentPrice());
        verify(assetDailyRepository, never()).save(any(AssetDaily.class));
    }

    @Test
    void getData_returnsCachedValueWhenProviderFailsAfterFirstSuccess() {
        stubHappyPathProviders();
        stubRepositoryReadsForMapping();
        when(assetDailyRepository.save(any(AssetDaily.class))).thenReturn(buildSavedDaily());
        stubCalculatorDtos();
        HypeDto cached = service.getData();

        when(hyperliquidClient.getHlData()).thenThrow(new RuntimeException("failed"));

        HypeDto result = service.getData();

        assertSame(cached, result);
    }

    private void stubHappyPathProviders() {
        when(hyperliquidClient.getHlData()).thenReturn(new HyperliquidDto(
                "1000",
                "100",
                "0.1",
                "200",
                "300",
                "0.2",
                "2000",
                "10",
                "500"));
        when(blockChainClient.getBlockchainData()).thenReturn(new BlockChainDto("100", "50"));
        when(coinGeckoClient.getData()).thenReturn(new CoinGeckoDto[] {
                new CoinGeckoDto("hype", 2d, 0d, 3d, 4d)
        });
    }

    private void stubRepositoryReadsForMapping() {
        AssetSnapshot snapshot = new AssetSnapshot();
        snapshot.setDay(1L);
        snapshot.setPrice(1d);
        snapshot.setFees24h(0.1d);
        snapshot.setBurnedHype("1");
        snapshot.setCirculatingSupply("10");
        when(assetSnapshotRepository.findFirstByOrderByDayDesc()).thenReturn(Optional.of(snapshot));
        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("HYPE")).thenReturn(List.of(snapshot));

        AssetDaily daily = new AssetDaily();
        daily.setLastRefresh(1L);
        daily.setCurrentPrice(1d);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("HYPE")).thenReturn(List.of(daily));
    }

    private AssetDaily buildSavedDaily() {
        AssetDaily daily = new AssetDaily();
        daily.setSymbol("HYPE");
        daily.setCurrentPrice(2d);
        daily.setMarketCap(2000d);
        daily.setPriceChangePercentage24h(3d);
        daily.setTotalVolume(4d);
        daily.setLastRefresh(123L);
        return daily;
    }

    private void stubCalculatorDtos() {
        when(hypeCalculator.computeTimedData(any(), any(), any())).thenReturn(new HypeTimedDataDto(
                1d, 2d, 3d, 4d, 5d, 6d, 7d, List.of(), List.of(), List.of(), List.of()));
        when(hypeCalculator.computeSupplyData(any())).thenReturn(new HypeSupplyDto(1d, 2d, 3d, 4d));
        when(hypeCalculator.computeBlockchainData(any(), any())).thenReturn(new HypeBlockchainDto(1d, 2d, 3d, 4d));
        when(hypeCalculator.computeHlpData(any())).thenReturn(new HypeHlpDto(1d, 2d, 3d));
        when(hypeCalculator.computeValuationData(any(), any(), any())).thenReturn(new HypeValuationDto(
                1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d, 10d, 11d));
    }
}
