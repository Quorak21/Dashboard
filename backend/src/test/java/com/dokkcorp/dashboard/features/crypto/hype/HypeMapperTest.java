package com.dokkcorp.dashboard.features.crypto.hype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
    void buildSummary_mapsEntityFields() {
        AssetDaily entity = new AssetDaily();
        entity.setSymbol("HYPE");
        entity.setCurrentPrice(2d);
        entity.setMarketCap(200d);
        entity.setPriceChangePercentage24h(3d);
        entity.setTotalVolume(4d);
        entity.setLastRefresh(Instant.ofEpochMilli(123L));

        var summary = mapper.buildSummary(entity);

        assertEquals("HYPE", summary.symbol());
        assertEquals(2d, summary.currentPrice());
    }

    @Test
    void buildTimedData_delegatesToCalculator() {
        AssetDaily entity = new AssetDaily();
        entity.setSymbol("HYPE");
        entity.setCurrentPrice(2d);
        entity.setMarketCap(200d);
        entity.setPriceChangePercentage24h(3d);
        entity.setTotalVolume(4d);
        entity.setLastRefresh(Instant.ofEpochMilli(123L));

        HyperliquidDto hyperliquidDto = new HyperliquidDto("1000", "100", "0.1", "200", "300", "0.2", "2000", "10", "500");
        List<AssetDaily> daily = List.of(entity);
        List<AssetSnapshot> history = List.of();

        when(hypeCalculator.computeTimedData(any(), any(), any())).thenReturn(new HypeTimedDataDto(
                1d, 2d, 3d, 4d, 5d, 6d, 7d, List.of(), List.of(), List.of(), List.of()));

        mapper.buildTimedData(hyperliquidDto, daily, history);

        verify(hypeCalculator).computeTimedData(any(), any(), any());
    }

    @Test
    void buildCharts_mapsActivitySeriesFromSnapshotsWithVolumeAndOpenInterest() {
        AssetSnapshot priceOnly = new AssetSnapshot();
        priceOnly.setDay(Instant.ofEpochMilli(10L));
        priceOnly.setPrice(0.1d);

        AssetSnapshot withActivity = new AssetSnapshot();
        withActivity.setDay(Instant.ofEpochMilli(20L));
        withActivity.setPrice(0.2d);
        withActivity.setVolume24h(1000d);
        withActivity.setOpenInterest(5000d);

        var charts = mapper.buildCharts(List.of(withActivity, priceOnly), List.of());

        assertEquals(List.of(1000d), charts.activityVolume());
        assertEquals(List.of(5000d), charts.activityOpenInterest());
        assertEquals(List.of(20L), charts.activityDays());
    }

    @Test
    void buildCharts_excludesSnapshotsWithoutVolumeOrOpenInterestFromActivitySeries() {
        AssetSnapshot missingOi = new AssetSnapshot();
        missingOi.setDay(Instant.ofEpochMilli(10L));
        missingOi.setPrice(0.1d);
        missingOi.setVolume24h(1000d);

        AssetSnapshot missingVolume = new AssetSnapshot();
        missingVolume.setDay(Instant.ofEpochMilli(20L));
        missingVolume.setPrice(0.2d);
        missingVolume.setOpenInterest(5000d);

        var charts = mapper.buildCharts(List.of(missingVolume, missingOi), List.of());

        assertTrue(charts.activityVolume().isEmpty());
        assertTrue(charts.activityOpenInterest().isEmpty());
        assertTrue(charts.activityDays().isEmpty());
    }

    @Test
    void buildSupply_delegatesToCalculator() {
        HyperliquidDto hyperliquidDto = new HyperliquidDto("1000", "100", "0.1", "200", "300", "0.2", "2000", "10", "500");
        when(hypeCalculator.computeSupplyData(any())).thenReturn(new HypeSupplyDto(1d, 2d, 3d, 4d));
        when(hypeCalculator.computeBlockchainData(any(), any())).thenReturn(new HypeBlockchainDto(1d, 2d, 3d, 4d));
        when(hypeCalculator.computeHlpData(any())).thenReturn(new HypeHlpDto(1d, 2d, 3d));

        mapper.buildSupply(hyperliquidDto);
        mapper.buildBlockchain(hyperliquidDto, new BlockChainDto("100", "50"));
        mapper.buildHlp(hyperliquidDto);

        verify(hypeCalculator).computeSupplyData(any());
        verify(hypeCalculator).computeBlockchainData(any(), any());
        verify(hypeCalculator).computeHlpData(any());
    }

    @Test
    void buildValuation_delegatesToCalculator() {
        AssetDaily entity = new AssetDaily();
        entity.setSymbol("HYPE");
        entity.setCurrentPrice(2d);
        entity.setMarketCap(200d);
        entity.setPriceChangePercentage24h(3d);
        entity.setTotalVolume(4d);
        entity.setLastRefresh(Instant.ofEpochMilli(123L));

        HyperliquidDto hyperliquidDto = new HyperliquidDto("1000", "100", "0.1", "200", "300", "0.2", "2000", "10", "500");
        when(hypeCalculator.computeValuationData(any(), any(), any())).thenReturn(new HypeValuationDto(
                1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d, 10d, 11d));

        mapper.buildValuation(hyperliquidDto, entity, List.of());

        verify(hypeCalculator).computeValuationData(any(), any(), any());
    }
}
