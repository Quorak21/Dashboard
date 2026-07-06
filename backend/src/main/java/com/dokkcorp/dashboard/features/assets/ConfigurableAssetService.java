package com.dokkcorp.dashboard.features.assets;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetDto;
import com.dokkcorp.dashboard.features.assets.model.AssetProvider;
import com.dokkcorp.dashboard.features.assets.model.MarketStatus;
import com.dokkcorp.dashboard.features.assets.model.PriceSource;
import com.dokkcorp.dashboard.features.assets.price.PriceProvider;
import com.dokkcorp.dashboard.features.assets.price.PriceProviderRegistry;
import com.dokkcorp.dashboard.features.assets.price.PriceQuote;
import com.dokkcorp.dashboard.exception.AssetNotFoundException;
import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;
import java.util.Locale;
import com.dokkcorp.dashboard.config.assets.AssetDividendsProperties;
import com.dokkcorp.dashboard.config.assets.AssetFundamentalsProperties;
import com.dokkcorp.dashboard.features.assets.alerts.QuarterlyReportAlertService;
import com.dokkcorp.dashboard.features.assets.model.DividendsBlock;
import com.dokkcorp.dashboard.features.assets.model.DividendHistoryEntry;
import com.dokkcorp.dashboard.features.assets.model.FundamentalsBlock;
import com.dokkcorp.dashboard.features.assets.model.HoldingEntry;
import com.dokkcorp.dashboard.features.assets.model.SectorWeight;

@Service
public class ConfigurableAssetService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurableAssetService.class);
    private static final long HISTORY_REFRESH_MS = 3_600_000L;

    private final AssetRegistry assetRegistry;
    private final PriceProviderRegistry priceProviderRegistry;
    private final AssetSnapshotRepository assetSnapshotRepository;
    private final AssetDailyRepository assetDailyRepository;
    private final MarketHoursGuard marketHoursGuard;
    private final ProviderCallMetrics providerCallMetrics;
    private final AssetDividendsProperties assetDividendsProperties;
    private final AssetFundamentalsProperties assetFundamentalsProperties;
    private final QuarterlyReportAlertService quarterlyReportAlertService;

    private final ConcurrentHashMap<String, AtomicReference<AssetDto>> cacheByAssetId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> syncLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HistoryState> historyStateByAssetId = new ConcurrentHashMap<>();

    public ConfigurableAssetService(
            AssetRegistry assetRegistry,
            PriceProviderRegistry priceProviderRegistry,
            AssetSnapshotRepository assetSnapshotRepository,
            AssetDailyRepository assetDailyRepository,
            MarketHoursGuard marketHoursGuard,
            ProviderCallMetrics providerCallMetrics,
            AssetDividendsProperties assetDividendsProperties,
            AssetFundamentalsProperties assetFundamentalsProperties,
            QuarterlyReportAlertService quarterlyReportAlertService) {
        this.assetRegistry = assetRegistry;
        this.priceProviderRegistry = priceProviderRegistry;
        this.assetSnapshotRepository = assetSnapshotRepository;
        this.assetDailyRepository = assetDailyRepository;
        this.marketHoursGuard = marketHoursGuard;
        this.providerCallMetrics = providerCallMetrics;
        this.assetDividendsProperties = assetDividendsProperties;
        this.assetFundamentalsProperties = assetFundamentalsProperties;
        this.quarterlyReportAlertService = quarterlyReportAlertService;
    }

    @PostConstruct
    void initializeCachesFromRegistry() {
        synchronizeCachesWithRegistry();
    }

    /**
     * Keeps in-memory maps aligned with the current registry.
     * Call again after a future registry reload.
     */
    void synchronizeCachesWithRegistry() {
        Set<String> registeredIds = assetRegistry.all().stream()
                .map(AssetDefinition::id)
                .collect(Collectors.toUnmodifiableSet());

        cacheByAssetId.keySet().removeIf(id -> !registeredIds.contains(id));
        syncLocks.keySet().removeIf(id -> !registeredIds.contains(id));
        historyStateByAssetId.keySet().removeIf(id -> !registeredIds.contains(id));

        for (String assetId : registeredIds) {
            cacheByAssetId.computeIfAbsent(assetId, id -> new AtomicReference<>());
            syncLocks.computeIfAbsent(assetId, id -> new Object());
            historyStateByAssetId.computeIfAbsent(assetId, id -> new HistoryState());
        }
    }

    public AssetDto getData(String assetId) {
        AssetDefinition asset = resolveAsset(assetId);
        refreshHistory(asset);
        AssetDto cached = cacheFor(assetId).get();
        if (cached == null) {
            return AssetDto.error(assetId, asset.dbSymbol());
        }
        return assembleDto(asset, cached, cached.priceSource(), marketHoursGuard.status(asset));
    }

    public AssetDto syncPrice(String assetId) {
        AssetDefinition asset = resolveAsset(assetId);
        Object lock = syncLocks.computeIfAbsent(assetId, id -> new Object());
        synchronized (lock) {
            try {
                refreshHistory(asset);
                if (asset.provider() == null) {
                    logger.warn("Asset {} has no provider configured", assetId);
                    return fallbackFromCache(asset);
                }
                PriceProvider provider = priceProviderRegistry.requireById(asset.provider().providerId());
                PriceQuote quote = provider.fetch(asset);

                if (quote == null || quote.fetchedAt() == null) {
                    if (asset.provider() == AssetProvider.SCRAPE) {
                        providerCallMetrics.incrementScrape();
                        providerCallMetrics.recordScrapeFailure();
                    }
                    return fallbackFromCache(asset);
                }

                if (marketHoursGuard.isOpen(asset)) {
                    try {
                        persistDailyPoint(asset, quote);
                    } catch (Exception e) {
                        logger.error("Failed to persist daily price point for asset {}", assetId, e);
                    }
                }

                LiveSeries live = buildLiveSeries(asset);
                HistoryState history = historyState(assetId);
                MarketStatus marketStatus = marketHoursGuard.status(asset);

                AssetDto dto = new AssetDto(
                        asset.id(),
                        asset.dbSymbol(),
                        asset.displayName(),
                        asset.type(),
                        quote.currency(),
                        quote.price(),
                        quote.marketCap(),
                        quote.changePercent24h(),
                        quote.volume(),
                        quote.fetchedAt().toEpochMilli(),
                        priceSourceFor(asset, false),
                        marketStatus,
                        history.historyPrices,
                        history.historyDays,
                        live.prices(),
                        live.days(),
                        buildDividendsBlock(asset.id(), quote.price()),
                        buildFundamentalsBlock(asset.id()));

                cacheFor(assetId).set(dto);
                if (asset.provider() == AssetProvider.FMP) {
                    providerCallMetrics.incrementFmp();
                } else if (asset.provider() == AssetProvider.SCRAPE) {
                    providerCallMetrics.incrementScrape();
                }
                return dto;
            } catch (Exception e) {
                logger.error("Price provider failed for asset {}", assetId, e);
                if (asset.provider() == AssetProvider.SCRAPE) {
                    providerCallMetrics.incrementScrape();
                    providerCallMetrics.recordScrapeFailure();
                }
                return fallbackFromCache(asset);
            }
        }
    }

    private AssetDto fallbackFromCache(AssetDefinition asset) {
        String assetId = asset.id();
        AssetDto stale = cacheFor(assetId).get();
        if (stale != null) {
            long ageMinutes = computeStaleAgeMinutes(stale);
            logger.warn("Stale price for {}, source=cache, age={}m", assetId, ageMinutes);
            AssetDto cached = assembleDto(asset, stale, PriceSource.CACHE, marketHoursGuard.status(asset));
            cacheFor(assetId).set(cached);
            return cached;
        }
        return AssetDto.error(assetId, asset.dbSymbol());
    }

    private long computeStaleAgeMinutes(AssetDto cached) {
        if (cached.lastRefresh() == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(
                Instant.ofEpochMilli(cached.lastRefresh()),
                Instant.now());
    }

    private AssetDto assembleDto(
            AssetDefinition asset,
            AssetDto priceFields,
            PriceSource priceSource,
            MarketStatus marketStatus) {
        List<Double> livePrices = priceFields.livePrices() != null ? priceFields.livePrices() : List.of();
        List<Long> liveDays = priceFields.liveDays() != null ? priceFields.liveDays() : List.of();
        DividendsBlock dividends = buildDividendsBlock(asset.id(), priceFields.currentPrice());
        FundamentalsBlock fundamentals = priceFields.fundamentals();
        HistoryState history = historyState(asset.id());
        
        return new AssetDto(
                asset.id(),
                asset.dbSymbol(),
                asset.displayName(),
                asset.type(),
                priceFields.currency(),
                priceFields.currentPrice(),
                priceFields.marketCap(),
                priceFields.priceChangePercentage24h(),
                priceFields.totalVolume(),
                priceFields.lastRefresh(),
                priceSource,
                marketStatus,
                history.historyPrices,
                history.historyDays,
                livePrices,
                liveDays,
                dividends,
                fundamentals);
    }

    private void persistDailyPoint(AssetDefinition asset, PriceQuote quote) {
        AssetDaily point = new AssetDaily();
        point.setSymbol(asset.dbSymbol());
        point.setCurrentPrice(quote.price());
        point.setMarketCap(quote.marketCap());
        point.setPriceChangePercentage24h(quote.changePercent24h());
        point.setTotalVolume(quote.volume());
        point.setLastRefresh(quote.fetchedAt());
        assetDailyRepository.save(point);
    }

    private LiveSeries buildLiveSeries(AssetDefinition asset) {
        if (asset.marketHours() == null || asset.marketHours().zone() == null) {
            return new LiveSeries(List.of(), List.of());
        }
        ZoneId zone = asset.marketHours().zone();
        List<AssetDaily> dailyPoints = assetDailyRepository
                .findTop144BySymbolOrderByLastRefreshDesc(asset.dbSymbol());
        List<AssetDaily> chronological = new ArrayList<>(dailyPoints);
        Collections.reverse(chronological);

        List<Double> livePrices = new ArrayList<>();
        List<Long> liveDays = new ArrayList<>();

        if (!chronological.isEmpty()) {
            AssetDaily latestPoint = null;
            for (int i = chronological.size() - 1; i >= 0; i--) {
                if (chronological.get(i).getLastRefresh() != null) {
                    latestPoint = chronological.get(i);
                    break;
                }
            }
            if (latestPoint == null) {
                return new LiveSeries(List.of(), List.of());
            }
            LocalDate latestLocalDate = latestPoint.getLastRefresh()
                    .atZone(zone)
                    .toLocalDate();

            for (AssetDaily point : chronological) {
                if (point.getLastRefresh() == null || point.getCurrentPrice() == null) {
                    continue;
                }
                LocalDate pointLocalDate = point.getLastRefresh()
                        .atZone(zone)
                        .toLocalDate();
                if (pointLocalDate.equals(latestLocalDate)) {
                    livePrices.add(point.getCurrentPrice());
                    liveDays.add(point.getLastRefresh().toEpochMilli());
                }
            }
        }

        return new LiveSeries(livePrices, liveDays);
    }

    private void refreshHistory(AssetDefinition asset) {
        String assetId = asset.id();
        HistoryState state = historyState(assetId);
        synchronized (state) {
            if (System.currentTimeMillis() - state.lastHistoryRefresh < HISTORY_REFRESH_MS) {
                return;
            }

            List<AssetSnapshot> snapshots = assetSnapshotRepository
                    .findTop365BySymbolOrderByDayDesc(asset.dbSymbol());
            if (snapshots == null) {
                return;
            }

            List<Double> prices = new ArrayList<>();
            List<Long> days = new ArrayList<>();
            for (int i = snapshots.size() - 1; i >= 0; i--) {
                AssetSnapshot snap = snapshots.get(i);
                if (snap == null || snap.getDay() == null || snap.getPrice() == null) {
                    continue;
                }
                prices.add(snap.getPrice());
                days.add(snap.getDay().toEpochMilli());
            }

            state.historyPrices = List.copyOf(prices);
            state.historyDays = List.copyOf(days);
            state.lastHistoryRefresh = System.currentTimeMillis();
        }
    }

    private DividendsBlock buildDividendsBlock(String assetId, Double currentPrice) {
        if (assetId == null) {
            return null;
        }
        AssetDividendsProperties.DividendsConfig config = assetDividendsProperties.getDividends()
                .get(assetId.toLowerCase(Locale.ROOT));
        if (config == null || config.getForwardDividend() == null) {
            return null;
        }
        
        List<DividendHistoryEntry> historyList = config.getHistory() == null ? List.of() : config.getHistory().stream()
                .filter(entry -> entry != null && entry.getAmount() != null && entry.getCurrency() != null)
                .map(entry -> new DividendHistoryEntry(entry.getYear(), entry.getAmount(), entry.getCurrency()))
                .collect(Collectors.toList());

        BigDecimal estimatedYield = null;
        if (currentPrice != null) {
            estimatedYield = computeEstimatedYield(assetId, BigDecimal.valueOf(currentPrice));
        }

        return new DividendsBlock(
                config.getForwardDividend(),
                config.getForwardDividendCurrency(),
                config.getFrequency(),
                estimatedYield,
                config.getAvgDividendGrowth10Y(),
                List.copyOf(historyList)
        );
    }

    private FundamentalsBlock buildFundamentalsBlock(String assetId) {
        if (assetId == null) {
            return null;
        }
        AssetFundamentalsProperties.FundamentalsConfig config = assetFundamentalsProperties.getFundamentals()
                .get(assetId.toLowerCase(Locale.ROOT));
        if (config == null) {
            return null;
        }

        List<HoldingEntry> holdingsList = config.getTopHoldings() == null ? List.of() : config.getTopHoldings().stream()
                .filter(entry -> entry != null && entry.getName() != null && entry.getWeightPercent() != null)
                .map(entry -> new HoldingEntry(entry.getName(), entry.getWeightPercent()))
                .collect(Collectors.toList());

        List<SectorWeight> sectorsList = config.getSectorWeights() == null ? List.of() : config.getSectorWeights().stream()
                .filter(entry -> entry != null && entry.getSector() != null && entry.getWeightPercent() != null)
                .map(entry -> new SectorWeight(entry.getSector(), entry.getWeightPercent()))
                .collect(Collectors.toList());

        boolean stale = quarterlyReportAlertService.isStale(config.getUpdatedAt());
        return new FundamentalsBlock(
                config.getUpdatedAt(),
                config.getSource(),
                stale,
                config.getMetrics() != null ? Map.copyOf(config.getMetrics()) : Map.of(),
                List.copyOf(holdingsList),
                List.copyOf(sectorsList)
        );
    }

    private AssetDefinition resolveAsset(String assetId) {
        return assetRegistry.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
    }

    private AtomicReference<AssetDto> cacheFor(String assetId) {
        return cacheByAssetId.computeIfAbsent(assetId, id -> new AtomicReference<>());
    }

    private HistoryState historyState(String assetId) {
        return historyStateByAssetId.computeIfAbsent(assetId, id -> new HistoryState());
    }

    private PriceSource priceSourceFor(AssetDefinition asset, boolean fromCache) {
        if (fromCache) return PriceSource.CACHE;
        if (asset.provider() == null) return PriceSource.CACHE;
        return switch (asset.provider()) {
            case FMP -> PriceSource.FMP;
            case SCRAPE -> PriceSource.SCRAPE;
        };
    }

    private static final class HistoryState {
        volatile List<Double> historyPrices = List.of();
        volatile List<Long> historyDays = List.of();
        volatile long lastHistoryRefresh = 0;
    }

    public BigDecimal computeEstimatedYield(String assetId, BigDecimal currentPrice) {
        if (assetId == null || currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        AssetDividendsProperties.DividendsConfig config = assetDividendsProperties.getDividends()
                .get(assetId.toLowerCase(Locale.ROOT));
        if (config == null || config.getForwardDividend() == null) {
            return null;
        }
        return config.getForwardDividend()
                .multiply(BigDecimal.valueOf(100))
                .divide(currentPrice, 2, RoundingMode.HALF_UP);
    }

    private record LiveSeries(List<Double> prices, List<Long> days) {
    }
}
