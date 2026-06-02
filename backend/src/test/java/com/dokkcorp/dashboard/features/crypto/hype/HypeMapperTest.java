package com.dokkcorp.dashboard.features.crypto.hype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dokkcorp.dashboard.features.crypto.hype.maths.HypeCalculator;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeBlockchainDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeHlpDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSupplyDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeTimedDataDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeValuationDto;
import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.providers.blockchain.BlockChainDto;
import com.dokkcorp.dashboard.providers.hyperliquid.HyperliquidDto;
import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

class HypeMapperTest {

    private final AssetDailyRepository assetDailyRepository = mock(AssetDailyRepository.class);
    private final AssetSnapshotRepository assetSnapshotRepository = mock(AssetSnapshotRepository.class);
    private final HypeCalculator hypeCalculator = mock(HypeCalculator.class);

    private final HypeMapper mapper = new HypeMapper(assetDailyRepository, assetSnapshotRepository, hypeCalculator);

    @Test
    void toDto_mapsSummaryAndDelegatesComputedSections() {
        AssetDaily entity = new AssetDaily();
        entity.setSymbol("HYPE");
        entity.setCurrentPrice(2d);
        entity.setMarketCap(200d);
        entity.setPriceChangePercentage24h(3d);
        entity.setTotalVolume(4d);
        entity.setLastRefresh(123L);

        AssetDaily previousDaily = new AssetDaily();
        previousDaily.setLastRefresh(100L);
        previousDaily.setCurrentPrice(1d);

        AssetSnapshot snapshot = new AssetSnapshot();
        snapshot.setDay(90L);
        snapshot.setPrice(0.5d);

        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("HYPE"))
                .thenReturn(List.of(previousDaily));
        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("HYPE"))
                .thenReturn(List.of(snapshot));

        when(hypeCalculator.computeTimedData(any(), any(), any())).thenReturn(new HypeTimedDataDto(
                1d, 2d, 3d, 4d, 5d, 6d, 7d, List.of(), List.of(), List.of(), List.of()));
        when(hypeCalculator.computeSupplyData(any())).thenReturn(new HypeSupplyDto(1d, 2d, 3d, 4d));
        when(hypeCalculator.computeBlockchainData(any(), any())).thenReturn(new HypeBlockchainDto(1d, 2d, 3d, 4d));
        when(hypeCalculator.computeHlpData(any())).thenReturn(new HypeHlpDto(1d, 2d, 3d));
        when(hypeCalculator.computeValuationData(any(), any(), any())).thenReturn(new HypeValuationDto(
                1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d, 10d, 11d));

        HypeDto result = mapper.toDto(
                entity,
                new HyperliquidDto("1000", "100", "0.1", "200", "300", "0.2", "2000", "10", "500"),
                new BlockChainDto("100", "50"));

        assertEquals("HYPE", result.summary().symbol());
        assertEquals(2d, result.summary().currentPrice());
        assertEquals(List.of(0.5d), result.charts().historyPrices());
        assertEquals(List.of(90L), result.charts().historyDays());
        verify(hypeCalculator).computeTimedData(any(), any(), any());
        verify(hypeCalculator).computeSupplyData(any());
        verify(hypeCalculator).computeBlockchainData(any(), any());
        verify(hypeCalculator).computeHlpData(any());
        verify(hypeCalculator).computeValuationData(any(), any(), any());
    }
}
