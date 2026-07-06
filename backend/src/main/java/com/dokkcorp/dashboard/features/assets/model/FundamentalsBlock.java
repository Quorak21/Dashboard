package com.dokkcorp.dashboard.features.assets.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record FundamentalsBlock(
        LocalDate updatedAt,
        String source,
        boolean stale,
        Map<String, Object> metrics,
        List<HoldingEntry> topHoldings,
        List<SectorWeight> sectorWeights) {
}
