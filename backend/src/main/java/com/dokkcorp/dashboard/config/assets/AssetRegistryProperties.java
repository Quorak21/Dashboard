package com.dokkcorp.dashboard.config.assets;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.assets-registry")
public class AssetRegistryProperties {

    private List<AssetEntry> entries = new ArrayList<>();

    public List<AssetEntry> getEntries() {
        return List.copyOf(entries);
    }

    public void setEntries(List<AssetEntry> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
    }

    public static class AssetEntry {
        private String id;
        private String displayName;
        private String provider;
        private String symbol;
        private String dbSymbol;
        private String type;
        private String currency;
        private MarketHoursEntry marketHours;
        private SyncEntry sync;
        private String scrapeParser;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getDbSymbol() {
            return dbSymbol;
        }

        public void setDbSymbol(String dbSymbol) {
            this.dbSymbol = dbSymbol;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public MarketHoursEntry getMarketHours() {
            return marketHours;
        }

        public void setMarketHours(MarketHoursEntry marketHours) {
            this.marketHours = marketHours;
        }

        public SyncEntry getSync() {
            return sync;
        }

        public void setSync(SyncEntry sync) {
            this.sync = sync;
        }

        public String getScrapeParser() {
            return scrapeParser;
        }

        public void setScrapeParser(String scrapeParser) {
            this.scrapeParser = scrapeParser;
        }
    }

    public static class MarketHoursEntry {
        private String zone;
        private String open;
        private String close;

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }

        public String getOpen() {
            return open;
        }

        public void setOpen(String open) {
            this.open = open;
        }

        public String getClose() {
            return close;
        }

        public void setClose(String close) {
            this.close = close;
        }
    }

    public static class SyncEntry {
        private Integer intervalMinutes;
        private Integer offsetMinutes;

        public Integer getIntervalMinutes() {
            return intervalMinutes;
        }

        public void setIntervalMinutes(Integer intervalMinutes) {
            this.intervalMinutes = intervalMinutes;
        }

        public Integer getOffsetMinutes() {
            return offsetMinutes;
        }

        public void setOffsetMinutes(Integer offsetMinutes) {
            this.offsetMinutes = offsetMinutes;
        }
    }
}
