package com.dokkcorp.dashboard.features.assets.alerts;

import java.time.LocalDate;

public record StaleAssetAlert(
        String assetId,
        String displayName,
        String label,
        LocalDate updatedAt,
        long daysStale) {
}
