package com.dokkcorp.dashboard.features.assets.alerts;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dokkcorp.dashboard.config.assets.AssetFundamentalsProperties;
import com.dokkcorp.dashboard.features.assets.AssetRegistry;
import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetType;

class QuarterlyReportAlertServiceTest {

    private AssetRegistry assetRegistry;
    private AssetFundamentalsProperties assetFundamentalsProperties;
    private Clock fixedClock;
    private QuarterlyReportAlertService alertService;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        assetRegistry = mock(AssetRegistry.class);
        assetFundamentalsProperties = mock(AssetFundamentalsProperties.class);

        // Fixed clock at 2026-06-25 UTC (matches ClockConfiguration.clock() which uses systemUTC)
        today = LocalDate.of(2026, 6, 25);
        Instant fixedInstant = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        // Clock is now a proper constructor param — no package-private workaround needed.
        alertService = new QuarterlyReportAlertService(
                assetRegistry,
                assetFundamentalsProperties,
                fixedClock,
                90
        );
    }

    // -------------------------------------------------------------------------
    // isStale() — boundary conditions
    // -------------------------------------------------------------------------

    @Test
    void testIsStale_thresholdValidation() {
        assertFalse(alertService.isStale(today.minusDays(89)), "89 days ago should NOT be stale");
        assertFalse(alertService.isStale(today.minusDays(90)), "Exactly 90 days ago should NOT be stale (strict >)");
        assertTrue(alertService.isStale(today.minusDays(91)),  "91 days ago SHOULD be stale");
        assertFalse(alertService.isStale(null), "null updatedAt should return false");
    }

    // -------------------------------------------------------------------------
    // getStaleAssets() — filtering, label, and boundary
    // -------------------------------------------------------------------------

    @Test
    void testGetStaleAssets_filtersAndCalculatesCorrectly() {
        AssetDefinition asset1 = new AssetDefinition(
                "asset1", "Asset One",   null, "A1", "A1", AssetType.STOCK, "USD", null, null, null);
        AssetDefinition asset2 = new AssetDefinition(
                "asset2", "Asset Two",   null, "A2", "A2", AssetType.STOCK, "USD", null, null, null);
        AssetDefinition asset3 = new AssetDefinition(
                "asset3", "Asset Three", null, "A3", "A3", AssetType.STOCK, "USD", null, null, null);
        AssetDefinition asset4 = new AssetDefinition(
                "asset4", "Asset Four",  null, "A4", "A4", AssetType.STOCK, "USD", null, null, null);

        when(assetRegistry.all()).thenReturn(List.of(asset1, asset2, asset3, asset4));

        Map<String, AssetFundamentalsProperties.FundamentalsConfig> fundamentalsMap = new HashMap<>();

        // asset1: fresh (89 days ago) — NOT stale
        AssetFundamentalsProperties.FundamentalsConfig config1 = new AssetFundamentalsProperties.FundamentalsConfig();
        config1.setAssetId("asset1");
        config1.setUpdatedAt(today.minusDays(89));
        fundamentalsMap.put("asset1", config1);

        // asset2: stale (91 days ago) — stale
        AssetFundamentalsProperties.FundamentalsConfig config2 = new AssetFundamentalsProperties.FundamentalsConfig();
        config2.setAssetId("asset2");
        config2.setUpdatedAt(today.minusDays(91));
        fundamentalsMap.put("asset2", config2);

        // asset3: no fundamentals config — skipped silently

        // asset4: config present but updatedAt is null — skipped silently
        AssetFundamentalsProperties.FundamentalsConfig config4 = new AssetFundamentalsProperties.FundamentalsConfig();
        config4.setAssetId("asset4");
        config4.setUpdatedAt(null);
        fundamentalsMap.put("asset4", config4);

        when(assetFundamentalsProperties.getFundamentals()).thenReturn(fundamentalsMap);

        List<StaleAssetAlert> staleAssets = alertService.getStaleAssets();

        assertEquals(1, staleAssets.size());
        StaleAssetAlert alert = staleAssets.get(0);
        assertEquals("asset2", alert.assetId());
        assertEquals("Asset Two", alert.displayName());
        assertEquals("A2", alert.label(), "label should be the asset symbol, not displayName");
        assertEquals(today.minusDays(91), alert.updatedAt());
        assertEquals(91, alert.daysStale());
    }

    @Test
    void testGetStaleAssets_atExactly90Days_isNotStale() {
        // P8: boundary test for getStaleAssets() at exactly the threshold.
        // This exercises the inline path in getStaleAssets() (delegated to isStale() after the fix).
        AssetDefinition asset = new AssetDefinition(
                "asset1", "Asset One", null, "A1", "A1", AssetType.STOCK, "USD", null, null, null);
        when(assetRegistry.all()).thenReturn(List.of(asset));

        Map<String, AssetFundamentalsProperties.FundamentalsConfig> fundamentalsMap = new HashMap<>();
        AssetFundamentalsProperties.FundamentalsConfig config = new AssetFundamentalsProperties.FundamentalsConfig();
        config.setAssetId("asset1");
        config.setUpdatedAt(today.minusDays(90)); // exactly 90 days — must NOT appear in alerts
        fundamentalsMap.put("asset1", config);
        when(assetFundamentalsProperties.getFundamentals()).thenReturn(fundamentalsMap);

        List<StaleAssetAlert> staleAssets = alertService.getStaleAssets();

        assertEquals(0, staleAssets.size(), "Exactly 90 days ago should not appear in the stale alert list");
    }

    @Test
    void testGetStaleAssets_nullRegistryResult_returnsEmpty() {
        when(assetRegistry.all()).thenReturn(null);
        when(assetFundamentalsProperties.getFundamentals()).thenReturn(Map.of());

        List<StaleAssetAlert> staleAssets = alertService.getStaleAssets();

        assertNotNull(staleAssets);
        assertEquals(0, staleAssets.size(), "Null registry result should produce an empty list, not NPE");
    }

    @Test
    void testGetStaleAssets_futureUpdatedAt_isSkipped() {
        AssetDefinition asset = new AssetDefinition(
                "asset1", "Asset One", null, "A1", "A1", AssetType.STOCK, "USD", null, null, null);
        when(assetRegistry.all()).thenReturn(List.of(asset));

        Map<String, AssetFundamentalsProperties.FundamentalsConfig> fundamentalsMap = new HashMap<>();
        AssetFundamentalsProperties.FundamentalsConfig config = new AssetFundamentalsProperties.FundamentalsConfig();
        config.setAssetId("asset1");
        config.setUpdatedAt(today.plusDays(10)); // future date — data integrity error
        fundamentalsMap.put("asset1", config);
        when(assetFundamentalsProperties.getFundamentals()).thenReturn(fundamentalsMap);

        List<StaleAssetAlert> staleAssets = alertService.getStaleAssets();

        assertEquals(0, staleAssets.size(), "Future updatedAt should be skipped with a warning log, not included");
    }
}
