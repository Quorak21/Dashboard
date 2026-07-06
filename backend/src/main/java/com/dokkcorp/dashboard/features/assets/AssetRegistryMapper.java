package com.dokkcorp.dashboard.features.assets;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import com.dokkcorp.dashboard.config.assets.AssetRegistryProperties;
import com.dokkcorp.dashboard.config.assets.AssetRegistryProperties.AssetEntry;
import com.dokkcorp.dashboard.config.assets.AssetRegistryProperties.MarketHoursEntry;
import com.dokkcorp.dashboard.config.assets.AssetRegistryProperties.SyncEntry;
import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetProvider;
import com.dokkcorp.dashboard.features.assets.model.AssetType;
import com.dokkcorp.dashboard.features.assets.model.MarketHours;
import com.dokkcorp.dashboard.features.assets.model.SyncConfig;

final class AssetRegistryMapper {

    private AssetRegistryMapper() {
    }

    static List<AssetDefinition> toDefinitions(List<AssetEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalStateException("assets-registry: at least one asset entry is required");
        }

        return entries.stream().map(AssetRegistryMapper::toDefinition).toList();
    }

    private static AssetDefinition toDefinition(AssetEntry entry) {
        if (entry == null) {
            throw new IllegalStateException("assets-registry: entry must not be null");
        }

        String id = requireText(entry.getId(), "id").toLowerCase(Locale.ROOT);
        String displayName = requireText(entry.getDisplayName(), "display-name");
        AssetProvider provider = parseProvider(entry.getProvider(), id);
        String symbol = requireText(entry.getSymbol(), "symbol");
        String dbSymbol = requireText(entry.getDbSymbol(), "db-symbol");
        AssetType type = parseType(entry.getType(), id);
        String currency = requireText(entry.getCurrency(), "currency");
        MarketHours marketHours = toMarketHours(entry.getMarketHours(), id);
        SyncConfig sync = toSync(entry.getSync(), id);
        String scrapeParser = entry.getScrapeParser();

        if (provider == AssetProvider.SCRAPE && (scrapeParser == null || scrapeParser.isBlank())) {
            throw new IllegalStateException(
                    "assets-registry entry '" + id + "': scrape-parser is required when provider is scrape");
        }
        if (provider == AssetProvider.FMP && scrapeParser != null && !scrapeParser.isBlank()) {
            throw new IllegalStateException(
                    "assets-registry entry '" + id + "': scrape-parser must be omitted when provider is fmp");
        }

        return new AssetDefinition(
                id,
                displayName,
                provider,
                symbol,
                dbSymbol,
                type,
                currency,
                marketHours,
                sync,
                scrapeParser != null && !scrapeParser.isBlank() ? scrapeParser.trim() : null);
    }

    private static MarketHours toMarketHours(MarketHoursEntry entry, String assetId) {
        if (entry == null) {
            throw new IllegalStateException("assets-registry entry '" + assetId + "': market-hours is required");
        }

        String zoneValue = requireText(entry.getZone(), "market-hours.zone");
        ZoneId zone;
        try {
            zone = ZoneId.of(zoneValue);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "assets-registry entry '" + assetId + "': invalid market-hours.zone '" + zoneValue + "'");
        }

        LocalTime open = parseTime(entry.getOpen(), assetId, "market-hours.open");
        LocalTime close = parseTime(entry.getClose(), assetId, "market-hours.close");
        if (!open.isBefore(close)) {
            throw new IllegalStateException(
                    "assets-registry entry '" + assetId + "': market-hours.open must be before market-hours.close");
        }

        return new MarketHours(zone, open, close);
    }

    private static SyncConfig toSync(SyncEntry entry, String assetId) {
        if (entry == null) {
            throw new IllegalStateException("assets-registry entry '" + assetId + "': sync is required");
        }
        if (entry.getIntervalMinutes() == null || entry.getIntervalMinutes() <= 0) {
            throw new IllegalStateException(
                    "assets-registry entry '" + assetId + "': sync.interval-minutes must be a positive integer");
        }
        if (entry.getOffsetMinutes() == null || entry.getOffsetMinutes() < 0) {
            throw new IllegalStateException(
                    "assets-registry entry '" + assetId + "': sync.offset-minutes must be zero or positive");
        }
        if (entry.getOffsetMinutes() >= entry.getIntervalMinutes()) {
            throw new IllegalStateException(
                    "assets-registry entry '" + assetId
                            + "': sync.offset-minutes must be less than sync.interval-minutes");
        }

        return new SyncConfig(entry.getIntervalMinutes(), entry.getOffsetMinutes());
    }

    private static LocalTime parseTime(String value, String assetId, String field) {
        String text = requireText(value, field);
        try {
            return LocalTime.parse(text);
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException(
                    "assets-registry entry '" + assetId + "': invalid " + field + " '" + text + "'");
        }
    }

    private static AssetProvider parseProvider(String value, String assetId) {
        try {
            return AssetProvider.fromYaml(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("assets-registry entry '" + assetId + "': " + ex.getMessage(), ex);
        }
    }

    private static AssetType parseType(String value, String assetId) {
        try {
            return AssetType.fromYaml(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("assets-registry entry '" + assetId + "': " + ex.getMessage(), ex);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("assets-registry: missing required field '" + field + "'");
        }
        return value.trim();
    }
}
