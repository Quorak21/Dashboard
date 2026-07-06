package com.dokkcorp.dashboard.config.assets;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssetFundamentalsProperties {

    private Map<String, FundamentalsConfig> fundamentals = new HashMap<>();

    public Map<String, FundamentalsConfig> getFundamentals() {
        return fundamentals;
    }

    public void setFundamentals(Map<String, FundamentalsConfig> fundamentals) {
        this.fundamentals = fundamentals != null ? fundamentals : new HashMap<>();
    }

    public static class FundamentalsConfig {
        private String assetId;
        private LocalDate updatedAt;
        private String source;
        private Map<String, Object> metrics = new HashMap<>();
        private List<HoldingProperties> topHoldings = new ArrayList<>();
        private List<SectorWeightProperties> sectorWeights = new ArrayList<>();
        private List<SectorWeightProperties> retailIndustryWeights = new ArrayList<>();

        public String getAssetId() {
            return assetId;
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }

        public LocalDate getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDate updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public Map<String, Object> getMetrics() {
            return metrics;
        }

        public void setMetrics(Map<String, Object> metrics) {
            this.metrics = metrics != null ? metrics : new HashMap<>();
        }

        public List<HoldingProperties> getTopHoldings() {
            return topHoldings;
        }

        public void setTopHoldings(List<HoldingProperties> topHoldings) {
            this.topHoldings = topHoldings != null ? topHoldings : new ArrayList<>();
        }

        public List<SectorWeightProperties> getSectorWeights() {
            return sectorWeights;
        }

        public void setSectorWeights(List<SectorWeightProperties> sectorWeights) {
            this.sectorWeights = sectorWeights != null ? sectorWeights : new ArrayList<>();
        }

        public List<SectorWeightProperties> getRetailIndustryWeights() {
            return retailIndustryWeights;
        }

        public void setRetailIndustryWeights(List<SectorWeightProperties> retailIndustryWeights) {
            this.retailIndustryWeights = retailIndustryWeights != null ? retailIndustryWeights : new ArrayList<>();
        }
    }

    public static class HoldingProperties {
        private String name;
        private BigDecimal weightPercent;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getWeightPercent() {
            return weightPercent;
        }

        public void setWeightPercent(BigDecimal weightPercent) {
            this.weightPercent = weightPercent;
        }
    }

    public static class SectorWeightProperties {
        private String sector;
        private BigDecimal weightPercent;

        public String getSector() {
            return sector;
        }

        public void setSector(String sector) {
            this.sector = sector;
        }

        public BigDecimal getWeightPercent() {
            return weightPercent;
        }

        public void setWeightPercent(BigDecimal weightPercent) {
            this.weightPercent = weightPercent;
        }
    }
}
