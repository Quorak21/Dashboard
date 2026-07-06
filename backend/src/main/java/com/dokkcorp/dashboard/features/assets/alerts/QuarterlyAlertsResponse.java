package com.dokkcorp.dashboard.features.assets.alerts;

import java.util.List;

public record QuarterlyAlertsResponse(
        List<StaleAssetAlert> alerts) {
}
