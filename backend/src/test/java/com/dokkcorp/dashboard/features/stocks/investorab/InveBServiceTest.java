package com.dokkcorp.dashboard.features.stocks.investorab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.providers.fmp.FMPClient;
import com.dokkcorp.dashboard.providers.fmp.FMPDto;
import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

class InveBServiceTest {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    private final FMPClient fmpClient = mock(FMPClient.class);
    private final AssetSnapshotRepository assetSnapshotRepository = mock(AssetSnapshotRepository.class);
    private final AssetDailyRepository assetDailyRepository = mock(AssetDailyRepository.class);
    private final InveBService service = new InveBService(fmpClient, assetSnapshotRepository, assetDailyRepository);

    @Test
    void getLastInveBData_returnsCachedValueWithoutExternalCalls() {
        stubFmp();
        stubHistorySnapshot(200d, 1_000L);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        service.getData();
        InveBDto cached = service.getLastInveBData();

        assertEquals("INVE-B", cached.symbol());
        assertEquals(245.5, cached.currentPrice());
        assertEquals(-1.25, cached.priceChangePercentage24h());
        assertEquals(List.of(200d), cached.historyPrices());
        assertEquals(List.of(1_000L), cached.historyDays());
        verify(fmpClient, times(1)).getData("INVE-B.ST");
    }

    @Test
    void getData_returnsErrorWhenFmpFailsAndNoCacheExists() {
        when(fmpClient.getData("INVE-B.ST")).thenThrow(new RuntimeException("down"));
        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("INVE-B"))
                .thenReturn(Collections.emptyList());
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        InveBDto result = service.getData();

        assertEquals("INVE-B", result.symbol());
        assertNull(result.currentPrice());
        verify(assetDailyRepository, never()).save(any(AssetDaily.class));
    }

    @Test
    void getData_keepsCachedDataWhenFmpFailsAfterFirstSuccess() {
        stubFmp();
        stubHistorySnapshot(200d, 1_000L);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        InveBDto first = service.getData();
        when(fmpClient.getData("INVE-B.ST")).thenThrow(new RuntimeException("down"));

        InveBDto second = service.getData();

        assertEquals(first.currentPrice(), second.currentPrice());
        assertEquals(first.historyPrices(), second.historyPrices());
    }

    @Test
    void getData_groupsLivePricesByLatestStockholmTradingDay() {
        stubFmp();
        stubHistorySnapshot(200d, 1_000L);

        Instant previousDay = LocalDate.of(2026, 6, 1).atTime(14, 0).atZone(STOCKHOLM).toInstant();
        Instant sameDayMorning = LocalDate.of(2026, 6, 2).atTime(10, 0).atZone(STOCKHOLM).toInstant();
        Instant sameDayAfternoon = LocalDate.of(2026, 6, 2).atTime(15, 0).atZone(STOCKHOLM).toInstant();

        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(List.of(
                        dailyPoint(sameDayAfternoon, 246d),
                        dailyPoint(sameDayMorning, 245d),
                        dailyPoint(previousDay, 240d)));

        InveBDto result = service.getData();

        assertEquals(List.of(245d, 246d), result.livePrices());
        assertEquals(List.of(sameDayMorning.toEpochMilli(), sameDayAfternoon.toEpochMilli()), result.liveDays());
    }

    private void stubFmp() {
        when(fmpClient.getData("INVE-B.ST")).thenReturn(new FMPDto[] {
                new FMPDto("INVE-B", 245.5, 1_000_000_000d, -1.25d, 123_456d)
        });
    }

    private void stubHistorySnapshot(double price, long dayMillis) {
        AssetSnapshot snapshot = new AssetSnapshot();
        snapshot.setPrice(price);
        snapshot.setDay(Instant.ofEpochMilli(dayMillis));
        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("INVE-B"))
                .thenReturn(List.of(snapshot));
    }

    private AssetDaily dailyPoint(Instant lastRefresh, double price) {
        AssetDaily daily = new AssetDaily();
        daily.setSymbol("INVE-B");
        daily.setCurrentPrice(price);
        daily.setLastRefresh(lastRefresh);
        return daily;
    }
}
