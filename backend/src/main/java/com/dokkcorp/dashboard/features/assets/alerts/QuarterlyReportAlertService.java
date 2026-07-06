package com.dokkcorp.dashboard.features.assets.alerts;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dokkcorp.dashboard.features.assets.AssetRegistry;
import com.dokkcorp.dashboard.config.assets.AssetFundamentalsProperties;
import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;

@Service
public class QuarterlyReportAlertService {

    private static final Logger log = LoggerFactory.getLogger(QuarterlyReportAlertService.class);

    private final AssetRegistry assetRegistry;
    private final AssetFundamentalsProperties assetFundamentalsProperties;
    private final Clock clock;
    private final int staleDaysThreshold;

    /**
     * Primary constructor — {@link Clock} is injected as a Spring {@code @Bean}
     * (see {@code ClockConfiguration}), making it mockable in any test context
     * including {@code @SpringBootTest} slices without reflection.
     */
    public QuarterlyReportAlertService(
            AssetRegistry assetRegistry,
            AssetFundamentalsProperties assetFundamentalsProperties,
            Clock clock,
            @Value("${app.alerts.fundamentals-stale-days:90}") int staleDaysThreshold) {
        this.assetRegistry = assetRegistry;
        this.assetFundamentalsProperties = assetFundamentalsProperties;
        this.clock = clock;
        // Guard: a threshold of 0 or negative would flag virtually every asset.
        this.staleDaysThreshold = Math.max(1, staleDaysThreshold);
    }

    /**
     * Returns the list of assets whose fundamentals have not been updated for more than
     * {@code staleDaysThreshold} days.
     *
     * <p>Assets without a fundamentals configuration or without an {@code updatedAt} date
     * are silently skipped (their data was never tracked).
     */
    public List<StaleAssetAlert> getStaleAssets() {
        List<StaleAssetAlert> alerts = new ArrayList<>();

        List<AssetDefinition> assets = assetRegistry.all();
        if (assets == null) {
            log.warn("QuarterlyReportAlertService: assetRegistry.all() returned null — returning empty alert list");
            return List.of();
        }

        Map<String, AssetFundamentalsProperties.FundamentalsConfig> fundamentalsMap =
                assetFundamentalsProperties.getFundamentals();

        for (AssetDefinition asset : assets) {
            String key = asset.id().toLowerCase(Locale.ROOT);
            AssetFundamentalsProperties.FundamentalsConfig config = fundamentalsMap.get(key);
            if (config == null || config.getUpdatedAt() == null) {
                continue;
            }

            LocalDate updatedAt = config.getUpdatedAt();
            long daysStale = ChronoUnit.DAYS.between(updatedAt, LocalDate.now(clock));

            if (daysStale < 0) {
                log.warn("QuarterlyReportAlertService: asset '{}' has a future updatedAt ({}) — data integrity issue, skipping",
                        asset.id(), updatedAt);
                continue;
            }

            // Delegate to isStale() — single source of truth for the staleness threshold.
            if (isStale(updatedAt)) {
                alerts.add(new StaleAssetAlert(
                        asset.id(),
                        asset.displayName(),
                        asset.symbol(),   // label = ticker symbol (distinct from displayName)
                        updatedAt,
                        daysStale
                ));
            }
        }
        return alerts;
    }

    /**
     * Returns {@code true} if the given {@code updatedAt} date is more than
     * {@code staleDaysThreshold} days in the past (relative to the injected {@link Clock}).
     *
     * @param updatedAt the date the fundamentals were last updated; {@code null} → {@code false}
     */
    public boolean isStale(LocalDate updatedAt) {
        if (updatedAt == null) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(updatedAt, LocalDate.now(clock));
        return days > staleDaysThreshold;
    }
}
