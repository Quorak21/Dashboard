package com.dokkcorp.dashboard.features.assets.model;

import java.math.BigDecimal;
import java.util.List;

public record DividendsBlock(
        BigDecimal forwardDividend,
        String forwardDividendCurrency,
        String frequency,
        BigDecimal estimatedYield,
        BigDecimal avgDividendGrowth10Y,
        List<DividendHistoryEntry> history) {
}
