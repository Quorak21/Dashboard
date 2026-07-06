package com.dokkcorp.dashboard.features.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetDto;
import com.dokkcorp.dashboard.features.assets.model.AssetProvider;
import com.dokkcorp.dashboard.features.assets.model.AssetType;
import com.dokkcorp.dashboard.features.assets.model.MarketHours;
import com.dokkcorp.dashboard.features.assets.model.MarketStatus;
import com.dokkcorp.dashboard.features.assets.model.PriceSource;
import com.dokkcorp.dashboard.features.assets.model.SyncConfig;
import com.dokkcorp.dashboard.features.assets.price.PriceProvider;
import com.dokkcorp.dashboard.features.assets.price.PriceProviderRegistry;
import com.dokkcorp.dashboard.features.assets.price.PriceQuote;
import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;
import com.dokkcorp.dashboard.config.assets.AssetDividendsProperties;
import com.dokkcorp.dashboard.config.assets.AssetFundamentalsProperties;
import com.dokkcorp.dashboard.features.assets.alerts.QuarterlyReportAlertService;
import com.dokkcorp.dashboard.features.assets.model.FundamentalsBlock;

class ConfigurableAssetServiceTest {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    private final AssetRegistry assetRegistry = mock(AssetRegistry.class);
    private final PriceProviderRegistry priceProviderRegistry = mock(PriceProviderRegistry.class);
    private final AssetSnapshotRepository assetSnapshotRepository = mock(AssetSnapshotRepository.class);
    private final AssetDailyRepository assetDailyRepository = mock(AssetDailyRepository.class);
    private final MarketHoursGuard marketHoursGuard = mock(MarketHoursGuard.class);
    private final ProviderCallMetrics providerCallMetrics = mock(ProviderCallMetrics.class);
    private final PriceProvider priceProvider = mock(PriceProvider.class);
    private final AssetDividendsProperties assetDividendsProperties = new AssetDividendsProperties();
    private final AssetFundamentalsProperties assetFundamentalsProperties = new AssetFundamentalsProperties();
    private final QuarterlyReportAlertService quarterlyReportAlertService = mock(QuarterlyReportAlertService.class);

    private AssetDefinition inveb;
    private AssetDefinition hypeScrape;
    private ConfigurableAssetService service;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger serviceLogger;

    @BeforeEach
    void setUp() {
        serviceLogger = (Logger) LoggerFactory.getLogger(ConfigurableAssetService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);

        inveb = new AssetDefinition(
                "inveb",
                "Investor AB",
                AssetProvider.FMP,
                "INVE-B.ST",
                "INVE-B",
                AssetType.STOCK,
                "SEK",
                new MarketHours(STOCKHOLM, LocalTime.of(9, 0), LocalTime.of(17, 35)),
                new SyncConfig(15, 0),
                null);

        hypeScrape = new AssetDefinition(
                "hype",
                "Hyperliquid",
                AssetProvider.SCRAPE,
                "HYPE",
                "HYPE",
                AssetType.TRUST,
                "USD",
                null,
                new SyncConfig(10, 0),
                null);

        when(assetRegistry.findById("inveb")).thenReturn(Optional.of(inveb));
        when(assetRegistry.findById("hype")).thenReturn(Optional.of(hypeScrape));
        when(assetRegistry.all()).thenReturn(List.of(inveb));
        when(priceProviderRegistry.requireById("fmp")).thenReturn(priceProvider);
        when(priceProviderRegistry.requireById("scrape")).thenReturn(priceProvider);

        service = new ConfigurableAssetService(
                assetRegistry,
                priceProviderRegistry,
                assetSnapshotRepository,
                assetDailyRepository,
                marketHoursGuard,
                providerCallMetrics,
                assetDividendsProperties,
                assetFundamentalsProperties,
                quarterlyReportAlertService);
        service.synchronizeCachesWithRegistry();
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logAppender);
    }

    @Test
    void syncPrice_persistsDailyAndUpdatesCache_whenMarketOpen() {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(true);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.OPEN);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        AssetDto result = service.syncPrice("inveb");

        assertEquals("inveb", result.assetId());
        assertEquals("INVE-B", result.symbol());
        assertEquals(245.5, result.currentPrice());
        assertEquals(1_000_000_000d, result.marketCap());
        assertEquals(-1.25, result.priceChangePercentage24h());
        assertEquals(123_456d, result.totalVolume());
        assertEquals(PriceSource.FMP, result.priceSource());
        assertEquals(MarketStatus.OPEN, result.marketStatus());
        assertNull(result.dividends());
        verify(assetDailyRepository).save(any(AssetDaily.class));
        verify(providerCallMetrics).incrementFmp();
    }

    @Test
    void syncPrice_skipsDailyWrite_whenMarketClosed() {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(false);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.CLOSED);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        service.syncPrice("inveb");

        verify(assetDailyRepository, never()).save(any(AssetDaily.class));
    }

    @Test
    void getData_warmsUpViaSyncPriceWhenCacheIsEmpty() {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(false);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.CLOSED);
        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("INVE-B"))
                .thenReturn(Collections.emptyList());
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        AssetDto result = service.getData("inveb");

        assertEquals("Investor AB", result.displayName());
        assertEquals(245.5, result.currentPrice());
        assertEquals(MarketStatus.CLOSED, result.marketStatus());
        verify(priceProvider, times(1)).fetch(inveb);
        verify(assetDailyRepository, never()).save(any(AssetDaily.class));
    }

    @Test
    void getData_returnsFundamentalsFromYaml_whenProviderFailsOnCacheMiss() {
        when(priceProvider.fetch(inveb)).thenThrow(new RuntimeException("FMP down"));
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.CLOSED);
        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        AssetFundamentalsProperties.FundamentalsConfig config = new AssetFundamentalsProperties.FundamentalsConfig();
        config.setAssetId("inveb");
        config.setUpdatedAt(LocalDate.of(2026, 4, 15));
        config.setSource("Q1 2026");
        config.getMetrics().put("trailing-pe", 6.11);
        assetFundamentalsProperties.getFundamentals().put("inveb", config);

        AssetDto result = service.getData("inveb");

        assertNull(result.currentPrice());
        assertNotNull(result.fundamentals());
        assertEquals(LocalDate.of(2026, 4, 15), result.fundamentals().updatedAt());
        assertEquals(6.11, result.fundamentals().metrics().get("trailing-pe"));
    }

    @Test
    void getData_returnsCacheWithoutProviderCall() {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(true);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.OPEN);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        service.syncPrice("inveb");
        AssetDto cached = service.getData("inveb");

        assertEquals("INVE-B", cached.symbol());
        assertEquals(245.5, cached.currentPrice());
        assertEquals(-1.25, cached.priceChangePercentage24h());
        assertEquals(List.of(200d), cached.historyPrices());
        assertEquals(List.of(1_000L), cached.historyDays());
        verify(priceProvider, times(1)).fetch(inveb);
    }

    @Test
    void getData_returnsStaleCacheAfterProviderFailure() {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(true);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.OPEN);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        service.syncPrice("inveb");
        when(priceProvider.fetch(inveb)).thenThrow(new RuntimeException("down"));
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.CLOSED);

        service.syncPrice("inveb");
        AssetDto result = service.getData("inveb");

        assertEquals(245.5, result.currentPrice());
        assertEquals(PriceSource.CACHE, result.priceSource());
        assertEquals(MarketStatus.CLOSED, result.marketStatus());
        verify(priceProvider, times(2)).fetch(inveb);
    }

    @Test
    void syncPrice_returnsStaleCacheOnProviderFailure() {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(true);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.OPEN);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        AssetDto first = service.syncPrice("inveb");
        when(priceProvider.fetch(inveb)).thenThrow(new RuntimeException("down"));
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.CLOSED);

        AssetDto second = service.syncPrice("inveb");

        assertEquals(first.currentPrice(), second.currentPrice());
        assertEquals(PriceSource.CACHE, second.priceSource());
        assertEquals(List.of(200d), second.historyPrices());
        verify(providerCallMetrics, times(1)).incrementFmp();
    }

    @Test
    void syncPrice_logsStaleCacheWarningOnProviderFailure() {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(true);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.OPEN);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        service.syncPrice("inveb");
        when(priceProvider.fetch(inveb)).thenThrow(new RuntimeException("down"));
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.CLOSED);

        service.syncPrice("inveb");

        assertTrue(logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .anyMatch(m -> m.contains("Stale price for inveb") && m.contains("source=cache")));
    }

    @Test
    void syncPrice_incrementsScrapeMetricsOnSuccessfulScrape() {
        when(priceProvider.fetch(hypeScrape)).thenReturn(new PriceQuote(
                42d, "USD", null, null, null, Instant.parse("2026-06-02T10:00:00Z")));
        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("HYPE"))
                .thenReturn(Collections.emptyList());
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("HYPE"))
                .thenReturn(Collections.emptyList());
        when(marketHoursGuard.isOpen(hypeScrape)).thenReturn(true);
        when(marketHoursGuard.status(hypeScrape)).thenReturn(MarketStatus.OPEN);

        service.syncPrice("hype");

        verify(providerCallMetrics).incrementScrape();
        verify(providerCallMetrics, never()).recordScrapeFailure();
    }

    @Test
    void syncPrice_recordsScrapeFailureOnException() {
        when(priceProvider.fetch(hypeScrape)).thenThrow(new RuntimeException("scrape down"));
        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("HYPE"))
                .thenReturn(Collections.emptyList());
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("HYPE"))
                .thenReturn(Collections.emptyList());

        service.syncPrice("hype");

        verify(providerCallMetrics).incrementScrape();
        verify(providerCallMetrics).recordScrapeFailure();
    }

    @Test
    void syncPrice_recordsScrapeFailureOnNullQuote() {
        when(priceProvider.fetch(hypeScrape)).thenReturn(null);
        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("HYPE"))
                .thenReturn(Collections.emptyList());
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("HYPE"))
                .thenReturn(Collections.emptyList());

        service.syncPrice("hype");

        verify(providerCallMetrics).incrementScrape();
        verify(providerCallMetrics).recordScrapeFailure();
    }

    @Test
    void syncPrice_returnsErrorWhenNoCacheAndProviderFails() {
        when(priceProvider.fetch(inveb)).thenThrow(new RuntimeException("down"));
        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("INVE-B"))
                .thenReturn(Collections.emptyList());
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        AssetDto result = service.syncPrice("inveb");

        assertEquals("inveb", result.assetId());
        assertEquals("INVE-B", result.symbol());
        assertNull(result.currentPrice());
        verify(assetDailyRepository, never()).save(any(AssetDaily.class));
    }

    @Test
    void getData_groupsLivePricesByLatestTradingDay() {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(true);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.OPEN);

        Instant previousDay = LocalDate.of(2026, 6, 1).atTime(14, 0).atZone(STOCKHOLM).toInstant();
        Instant sameDayMorning = LocalDate.of(2026, 6, 2).atTime(10, 0).atZone(STOCKHOLM).toInstant();
        Instant sameDayAfternoon = LocalDate.of(2026, 6, 2).atTime(15, 0).atZone(STOCKHOLM).toInstant();

        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(List.of(
                        dailyPoint(sameDayAfternoon, 246d),
                        dailyPoint(sameDayMorning, 245d),
                        dailyPoint(previousDay, 240d)));

        AssetDto result = service.syncPrice("inveb");

        assertEquals(List.of(245d, 246d), result.livePrices());
        assertEquals(List.of(sameDayMorning.toEpochMilli(), sameDayAfternoon.toEpochMilli()), result.liveDays());
    }

    @Test
    void synchronizeCachesWithRegistry_initializesOnlyRegisteredAssetKeys() {
        assertEquals(Set.of("inveb"), cacheKeys(service));
        assertEquals(Set.of("inveb"), syncLockKeys(service));
        assertEquals(Set.of("inveb"), historyStateKeys(service));
    }

    @Test
    void synchronizeCachesWithRegistry_evictsOrphanedEntries() throws Exception {
        AssetDefinition removed = new AssetDefinition(
                "removed",
                "Removed Asset",
                AssetProvider.FMP,
                "REM.ST",
                "REM",
                AssetType.STOCK,
                "SEK",
                new MarketHours(STOCKHOLM, LocalTime.of(9, 0), LocalTime.of(17, 35)),
                new SyncConfig(15, 0),
                null);

        when(assetRegistry.all()).thenReturn(List.of(inveb, removed));
        when(assetRegistry.findById("removed")).thenReturn(Optional.of(removed));
        service.synchronizeCachesWithRegistry();

        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(true);
        when(marketHoursGuard.isOpen(removed)).thenReturn(true);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.OPEN);
        when(marketHoursGuard.status(removed)).thenReturn(MarketStatus.OPEN);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("REM"))
                .thenReturn(Collections.emptyList());
        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("REM"))
                .thenReturn(Collections.emptyList());

        service.syncPrice("inveb");
        service.syncPrice("removed");

        assertTrue(cacheKeys(service).contains("removed"));

        when(assetRegistry.all()).thenReturn(List.of(inveb));
        service.synchronizeCachesWithRegistry();

        assertEquals(Set.of("inveb"), cacheKeys(service));
        assertEquals(Set.of("inveb"), syncLockKeys(service));
        assertEquals(Set.of("inveb"), historyStateKeys(service));
    }

    @SuppressWarnings("unchecked")
    private Set<String> cacheKeys(ConfigurableAssetService target) {
        return mapKeys(target, "cacheByAssetId");
    }

    @SuppressWarnings("unchecked")
    private Set<String> syncLockKeys(ConfigurableAssetService target) {
        return mapKeys(target, "syncLocks");
    }

    @SuppressWarnings("unchecked")
    private Set<String> historyStateKeys(ConfigurableAssetService target) {
        return mapKeys(target, "historyStateByAssetId");
    }

    @SuppressWarnings("unchecked")
    private Set<String> mapKeys(ConfigurableAssetService target, String fieldName) {
        try {
            Field field = ConfigurableAssetService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            ConcurrentHashMap<String, ?> map = (ConcurrentHashMap<String, ?>) field.get(target);
            return Set.copyOf(map.keySet());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private void stubProviderQuote() {
        when(priceProvider.fetch(inveb)).thenReturn(new PriceQuote(
                245.5,
                "SEK",
                1_000_000_000d,
                -1.25d,
                123_456d,
                Instant.parse("2026-06-02T10:00:00Z")));
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

    @Test
    void syncPrice_populatesDividendsBlock_whenConfigPresent() {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(true);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.OPEN);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        // Setup dividend config
        AssetDividendsProperties.DividendsConfig config = new AssetDividendsProperties.DividendsConfig();
        config.setAssetId("inveb");
        config.setForwardDividend(java.math.BigDecimal.valueOf(6.0));
        config.setForwardDividendCurrency("SEK");
        config.setFrequency("annual");
        config.setAvgDividendGrowth10Y(java.math.BigDecimal.valueOf(8.2));
        
        AssetDividendsProperties.DividendHistoryEntry historyEntry = new AssetDividendsProperties.DividendHistoryEntry();
        historyEntry.setYear(2024);
        historyEntry.setAmount(java.math.BigDecimal.valueOf(6.0));
        historyEntry.setCurrency("SEK");
        config.setHistory(List.of(historyEntry));

        assetDividendsProperties.getDividends().put("inveb", config);

        AssetDto result = service.syncPrice("inveb");

        assertEquals("inveb", result.assetId());
        org.junit.jupiter.api.Assertions.assertNotNull(result.dividends());
        assertEquals(java.math.BigDecimal.valueOf(6.0), result.dividends().forwardDividend());
        assertEquals("SEK", result.dividends().forwardDividendCurrency());
        assertEquals("annual", result.dividends().frequency());
        assertEquals(java.math.BigDecimal.valueOf(8.2), result.dividends().avgDividendGrowth10Y());
        assertEquals(1, result.dividends().history().size());
        assertEquals(2024, result.dividends().history().get(0).year());
        assertEquals(java.math.BigDecimal.valueOf(6.0), result.dividends().history().get(0).amount());
        assertEquals("SEK", result.dividends().history().get(0).currency());
        assertEquals(BigDecimal.valueOf(2.44), result.dividends().estimatedYield());
    }

    @Test
    void syncPrice_populatesFundamentalsBlock_whenConfigPresent() {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(true);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.OPEN);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        // Setup fundamentals config
        AssetFundamentalsProperties.FundamentalsConfig config = new AssetFundamentalsProperties.FundamentalsConfig();
        config.setAssetId("inveb");
        config.setUpdatedAt(LocalDate.of(2026, 4, 15));
        config.setSource("Source: Q1 2026 report");
        config.getMetrics().put("trailing-pe", 6.11);
        config.getMetrics().put("debt-leverage", "1.2%");

        AssetFundamentalsProperties.HoldingProperties holding = new AssetFundamentalsProperties.HoldingProperties();
        holding.setName("ABB");
        holding.setWeightPercent(BigDecimal.valueOf(16.5));
        config.setTopHoldings(List.of(holding));

        AssetFundamentalsProperties.SectorWeightProperties sector = new AssetFundamentalsProperties.SectorWeightProperties();
        sector.setSector("Industrials");
        sector.setWeightPercent(BigDecimal.valueOf(45.2));
        config.setSectorWeights(List.of(sector));

        assetFundamentalsProperties.getFundamentals().put("inveb", config);

        AssetDto result = service.syncPrice("inveb");

        assertEquals("inveb", result.assetId());
        assertNotNull(result.fundamentals());
        assertEquals(LocalDate.of(2026, 4, 15), result.fundamentals().updatedAt());
        assertEquals("Source: Q1 2026 report", result.fundamentals().source());
        assertEquals(6.11, result.fundamentals().metrics().get("trailing-pe"));
        assertEquals("1.2%", result.fundamentals().metrics().get("debt-leverage"));
        assertFalse(result.fundamentals().stale());

        assertEquals(1, result.fundamentals().topHoldings().size());
        assertEquals("ABB", result.fundamentals().topHoldings().get(0).name());
        assertEquals(BigDecimal.valueOf(16.5), result.fundamentals().topHoldings().get(0).weightPercent());

        assertEquals(1, result.fundamentals().sectorWeights().size());
        assertEquals("Industrials", result.fundamentals().sectorWeights().get(0).sector());
        assertEquals(BigDecimal.valueOf(45.2), result.fundamentals().sectorWeights().get(0).weightPercent());
        assertTrue(result.fundamentals().retailIndustryWeights().isEmpty());
    }

    @Test
    void computeEstimatedYield_calculatesYieldCorrectly() {
        AssetDividendsProperties.DividendsConfig config = new AssetDividendsProperties.DividendsConfig();
        config.setAssetId("inveb");
        config.setForwardDividend(BigDecimal.valueOf(6.0));
        assetDividendsProperties.getDividends().put("inveb", config);

        BigDecimal yield = service.computeEstimatedYield("inveb", BigDecimal.valueOf(240.0));
        assertNotNull(yield);
        assertEquals(0, BigDecimal.valueOf(2.50).setScale(2).compareTo(yield));
    }

    @Test
    void computeEstimatedYield_returnsNull_whenPriceIsNull() {
        AssetDividendsProperties.DividendsConfig config = new AssetDividendsProperties.DividendsConfig();
        config.setAssetId("inveb");
        config.setForwardDividend(BigDecimal.valueOf(6.0));
        assetDividendsProperties.getDividends().put("inveb", config);

        BigDecimal yieldNullPrice = service.computeEstimatedYield("inveb", null);
        assertNull(yieldNullPrice);

        BigDecimal yieldZeroPrice = service.computeEstimatedYield("inveb", BigDecimal.ZERO);
        assertNull(yieldZeroPrice);
    }

    @Test
    void computeEstimatedYield_returnsNull_whenNoDividendConfig() {
        BigDecimal yield = service.computeEstimatedYield("inveb", BigDecimal.valueOf(240.0));
        assertNull(yield);
    }

    @Test
    void getData_recomputesDividendsBlock_whenConfigPresent() {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(true);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.OPEN);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        AssetDividendsProperties.DividendsConfig config = new AssetDividendsProperties.DividendsConfig();
        config.setAssetId("inveb");
        config.setForwardDividend(java.math.BigDecimal.valueOf(6.0));
        config.setForwardDividendCurrency("SEK");
        config.setFrequency("annual");
        config.setAvgDividendGrowth10Y(java.math.BigDecimal.valueOf(8.2));
        
        assetDividendsProperties.getDividends().put("inveb", config);

        service.syncPrice("inveb");

        config.setForwardDividend(java.math.BigDecimal.valueOf(12.0));

        AssetDto result = service.getData("inveb");

        assertNotNull(result.dividends());
        assertEquals(java.math.BigDecimal.valueOf(12.0), result.dividends().forwardDividend());
        assertEquals(BigDecimal.valueOf(4.89), result.dividends().estimatedYield());
    }

    @Test
    void buildDividendsBlock_returnsNull_whenForwardDividendNotConfigured() {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(true);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.OPEN);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        AssetDividendsProperties.DividendsConfig config = new AssetDividendsProperties.DividendsConfig();
        config.setAssetId("inveb");
        config.setFrequency("annual");
        assetDividendsProperties.getDividends().put("inveb", config);

        AssetDto result = service.syncPrice("inveb");

        assertNull(result.dividends());
    }

    @Test
    void syncPrice_fallsBackToCache_whenRefreshHistoryThrowsException() throws Exception {
        stubProviderQuote();
        stubHistorySnapshot(200d, 1_000L);
        when(marketHoursGuard.isOpen(inveb)).thenReturn(true);
        when(marketHoursGuard.status(inveb)).thenReturn(MarketStatus.OPEN);
        when(assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("INVE-B"))
                .thenReturn(Collections.emptyList());

        service.syncPrice("inveb");

        // Force history refresh to run again by clearing history state cache
        java.lang.reflect.Field field = ConfigurableAssetService.class.getDeclaredField("historyStateByAssetId");
        field.setAccessible(true);
        ((java.util.Map<?, ?>) field.get(service)).clear();

        when(assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("INVE-B"))
                .thenThrow(new RuntimeException("Database error"));

        AssetDto result = service.syncPrice("inveb");

        assertNotNull(result);
        assertEquals("inveb", result.assetId());
        assertEquals(PriceSource.CACHE, result.priceSource());
    }
}
