package com.dokkcorp.dashboard.features.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetProvider;
import com.dokkcorp.dashboard.features.assets.model.AssetType;
import com.dokkcorp.dashboard.features.assets.model.MarketHours;
import com.dokkcorp.dashboard.features.assets.model.MarketStatus;
import com.dokkcorp.dashboard.features.assets.model.SyncConfig;

class MarketHoursGuardTest {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");
    private static final ZoneId LONDON = ZoneId.of("Europe/London");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId ZURICH = ZoneId.of("Europe/Zurich");

    private final AssetDefinition inveb = new AssetDefinition(
            "inveb",
            "Investor AB",
            AssetProvider.FMP,
            "INVE-B.ST",
            "INVE-B",
            AssetType.STOCK,
            "SEK",
            new MarketHours(STOCKHOLM, LocalTime.of(9, 0), LocalTime.of(17, 35)),
            new SyncConfig(15, 0),
            null);

    private final AssetDefinition londonAsset = new AssetDefinition(
            "lse-test",
            "London Test",
            AssetProvider.FMP,
            "LSE.L",
            "LSE",
            AssetType.STOCK,
            "GBP",
            new MarketHours(LONDON, LocalTime.of(8, 0), LocalTime.of(16, 30)),
            new SyncConfig(15, 0),
            null);

    private final AssetDefinition o = new AssetDefinition(
            "o",
            "Realty",
            AssetProvider.FMP,
            "O",
            "O",
            AssetType.STOCK,
            "USD",
            new MarketHours(NEW_YORK, LocalTime.of(9, 30), LocalTime.of(16, 0)),
            new SyncConfig(15, 0),
            null);

    private final AssetDefinition chdiv = new AssetDefinition(
            "chdiv",
            "UBS MSCI Swiss Dividend ETF",
            AssetProvider.SCRAPE,
            "CHDIV.SW",
            "CHDIV",
            AssetType.ETF,
            "CHF",
            new MarketHours(ZURICH, LocalTime.of(9, 0), LocalTime.of(17, 30)),
            new SyncConfig(10, 0),
            "yahoo-six-chart");

    @Test
    void isOpen_returnsFalseOnSaturday() {
        Clock saturday = fixedClock(LocalDate.of(2026, 6, 6), 10, 0);
        MarketHoursGuard guard = new MarketHoursGuard(saturday);

        assertFalse(guard.isOpen(inveb));
        assertEquals(MarketStatus.CLOSED, guard.status(inveb));
    }

    @Test
    void isOpen_returnsTrueOnTuesdayMorning() {
        Clock tuesdayMorning = fixedClock(LocalDate.of(2026, 6, 2), 10, 0);
        MarketHoursGuard guard = new MarketHoursGuard(tuesdayMorning);

        assertTrue(guard.isOpen(inveb));
        assertEquals(MarketStatus.OPEN, guard.status(inveb));
    }

    @Test
    void isOpen_returnsFalseOnTuesdayEvening() {
        Clock tuesdayEvening = fixedClock(LocalDate.of(2026, 6, 2), 18, 0);
        MarketHoursGuard guard = new MarketHoursGuard(tuesdayEvening);

        assertFalse(guard.isOpen(inveb));
        assertEquals(MarketStatus.CLOSED, guard.status(inveb));
    }

    @Test
    void isOpen_londonMarketBoundaryChecks() {
        // LSE (London): Open 08:00 - Close 16:30. Fuseau Europe/London
        LocalDate monday = LocalDate.of(2026, 6, 8);

        // Avant ouverture (07:59)
        Clock beforeOpen = fixedClock(monday, 7, 59, LONDON);
        assertFalse(new MarketHoursGuard(beforeOpen).isOpen(londonAsset));

        // À l'ouverture (08:00)
        Clock atOpen = fixedClock(monday, 8, 0, LONDON);
        assertTrue(new MarketHoursGuard(atOpen).isOpen(londonAsset));

        // Juste avant la fermeture (16:30)
        Clock beforeClose = fixedClock(monday, 16, 30, LONDON);
        assertTrue(new MarketHoursGuard(beforeClose).isOpen(londonAsset));

        // Après fermeture (16:31)
        Clock afterClose = fixedClock(monday, 16, 31, LONDON);
        assertFalse(new MarketHoursGuard(afterClose).isOpen(londonAsset));

        // Week-end (Dimanche)
        LocalDate sunday = LocalDate.of(2026, 6, 7);
        Clock weekend = fixedClock(sunday, 12, 0, LONDON);
        assertFalse(new MarketHoursGuard(weekend).isOpen(londonAsset));
    }

    @Test
    void isOpen_newYorkMarketBoundaryChecks() {
        // NYSE (New York): Open 09:30 - Close 16:00. Fuseau America/New_York
        LocalDate wednesday = LocalDate.of(2026, 6, 10);

        // Avant ouverture (09:29)
        Clock beforeOpen = fixedClock(wednesday, 9, 29, NEW_YORK);
        assertFalse(new MarketHoursGuard(beforeOpen).isOpen(o));

        // À l'ouverture (09:30)
        Clock atOpen = fixedClock(wednesday, 9, 30, NEW_YORK);
        assertTrue(new MarketHoursGuard(atOpen).isOpen(o));

        // Juste avant la fermeture (16:00)
        Clock beforeClose = fixedClock(wednesday, 16, 0, NEW_YORK);
        assertTrue(new MarketHoursGuard(beforeClose).isOpen(o));

        // Après fermeture (16:01)
        Clock afterClose = fixedClock(wednesday, 16, 1, NEW_YORK);
        assertFalse(new MarketHoursGuard(afterClose).isOpen(o));

        // Week-end (Samedi)
        LocalDate saturday = LocalDate.of(2026, 6, 6);
        Clock weekend = fixedClock(saturday, 12, 0, NEW_YORK);
        assertFalse(new MarketHoursGuard(weekend).isOpen(o));
    }

    @Test
    void isOpen_zurichMarketBoundaryChecks() {
        // SIX (Zurich): Open 09:00 - Close 17:30. Fuseau Europe/Zurich
        LocalDate friday = LocalDate.of(2026, 6, 12);

        // Avant ouverture (08:59)
        Clock beforeOpen = fixedClock(friday, 8, 59, ZURICH);
        assertFalse(new MarketHoursGuard(beforeOpen).isOpen(chdiv));

        // À l'ouverture (09:00)
        Clock atOpen = fixedClock(friday, 9, 0, ZURICH);
        assertTrue(new MarketHoursGuard(atOpen).isOpen(chdiv));

        // Juste avant la fermeture (17:30)
        Clock beforeClose = fixedClock(friday, 17, 30, ZURICH);
        assertTrue(new MarketHoursGuard(beforeClose).isOpen(chdiv));

        // Après fermeture (17:31)
        Clock afterClose = fixedClock(friday, 17, 31, ZURICH);
        assertFalse(new MarketHoursGuard(afterClose).isOpen(chdiv));

        // Week-end (Samedi)
        LocalDate saturday = LocalDate.of(2026, 6, 6);
        Clock weekend = fixedClock(saturday, 12, 0, ZURICH);
        assertFalse(new MarketHoursGuard(weekend).isOpen(chdiv));
    }

    private static Clock fixedClock(LocalDate date, int hour, int minute) {
        return fixedClock(date, hour, minute, STOCKHOLM);
    }

    private static Clock fixedClock(LocalDate date, int hour, int minute, ZoneId zone) {
        Instant instant = date.atTime(hour, minute).atZone(zone).toInstant();
        return Clock.fixed(instant, zone);
    }
}
