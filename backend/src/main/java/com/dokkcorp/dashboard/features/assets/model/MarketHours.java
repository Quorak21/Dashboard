package com.dokkcorp.dashboard.features.assets.model;

import java.time.LocalTime;
import java.time.ZoneId;

public record MarketHours(ZoneId zone, LocalTime open, LocalTime close) {
}
