package com.dokkcorp.dashboard.config.assets;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssetDividendsProperties {

    private Map<String, DividendsConfig> dividends = new HashMap<>();

    public Map<String, DividendsConfig> getDividends() {
        return dividends;
    }

    public void setDividends(Map<String, DividendsConfig> dividends) {
        this.dividends = dividends != null ? dividends : new HashMap<>();
    }

    public static class DividendsConfig {
        private String assetId;
        private BigDecimal forwardDividend;
        private String forwardDividendCurrency;
        private String frequency;
        private BigDecimal avgDividendGrowth10Y;
        private List<DividendHistoryEntry> history = new ArrayList<>();

        public String getAssetId() {
            return assetId;
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }

        public BigDecimal getForwardDividend() {
            return forwardDividend;
        }

        public void setForwardDividend(BigDecimal forwardDividend) {
            this.forwardDividend = forwardDividend;
        }

        public String getForwardDividendCurrency() {
            return forwardDividendCurrency;
        }

        public void setForwardDividendCurrency(String forwardDividendCurrency) {
            this.forwardDividendCurrency = forwardDividendCurrency;
        }

        public String getFrequency() {
            return frequency;
        }

        public void setFrequency(String frequency) {
            this.frequency = frequency;
        }

        public BigDecimal getAvgDividendGrowth10Y() {
            return avgDividendGrowth10Y;
        }

        public void setAvgDividendGrowth10Y(BigDecimal avgDividendGrowth10Y) {
            this.avgDividendGrowth10Y = avgDividendGrowth10Y;
        }

        public List<DividendHistoryEntry> getHistory() {
            return history;
        }

        public void setHistory(List<DividendHistoryEntry> history) {
            this.history = history != null ? history : new ArrayList<>();
        }
    }

    public static class DividendHistoryEntry {
        private int year;
        private BigDecimal amount;
        private String currency;

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}
