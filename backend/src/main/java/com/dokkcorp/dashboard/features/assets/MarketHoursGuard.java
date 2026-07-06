package com.dokkcorp.dashboard.features.assets;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.MarketHours;
import com.dokkcorp.dashboard.features.assets.model.MarketStatus;

@Component
public class MarketHoursGuard {

    private final Clock clock;

    public MarketHoursGuard() {
        this(Clock.systemDefaultZone());
    }

    MarketHoursGuard(Clock clock) {
        this.clock = clock;
    }

    public boolean isOpen(AssetDefinition asset) {
        MarketHours hours = asset.marketHours();
        if (hours == null) {
            return true; // actif 24/7 : pas de restriction horaire configurée
        }
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(hours.zone()));
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();
        return day != DayOfWeek.SATURDAY
                && day != DayOfWeek.SUNDAY
                && !time.isBefore(hours.open())
                && !time.isAfter(hours.close());
    }

    public MarketStatus status(AssetDefinition asset) {
        return isOpen(asset) ? MarketStatus.OPEN : MarketStatus.CLOSED;
    }
}
