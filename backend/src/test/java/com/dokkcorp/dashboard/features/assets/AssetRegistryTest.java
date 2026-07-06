package com.dokkcorp.dashboard.features.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.dokkcorp.dashboard.config.assets.AssetRegistryProperties;
import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetProvider;
import com.dokkcorp.dashboard.features.assets.model.AssetType;
import com.dokkcorp.dashboard.features.assets.model.MarketHours;
import com.dokkcorp.dashboard.features.assets.model.SyncConfig;

@SpringBootTest
class AssetRegistryTest {

    @Autowired
    private AssetRegistry assetRegistry;

    @Test
    void loadsInvebFromYamlRegistry() {
        Optional<AssetDefinition> inveb = assetRegistry.findById("inveb");

        assertTrue(inveb.isPresent());
        AssetDefinition asset = inveb.get();
        assertEquals("inveb", asset.id());
        assertEquals("Investor AB", asset.displayName());
        assertEquals(AssetProvider.FMP, asset.provider());
        assertEquals("INVE-B.ST", asset.symbol());
        assertEquals("INVE-B", asset.dbSymbol());
        assertEquals(AssetType.STOCK, asset.type());
        assertEquals("SEK", asset.currency());
        assertEquals(new MarketHours(ZoneId.of("Europe/Stockholm"), LocalTime.of(9, 0), LocalTime.of(17, 35)),
                asset.marketHours());
        assertEquals(new SyncConfig(15, 0), asset.sync());
    }

    @Test
    void findByIdIsCaseInsensitive() {
        assertTrue(assetRegistry.findById("INVEB").isPresent());
        assertTrue(assetRegistry.findById("Inveb").isPresent());
    }

    @Test
    void rejectsInvalidMarketHoursOnStartupMapping() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> DefaultAssetRegistry.fromProperties(propertiesWith(entry -> {
                    entry.setMarketHours(invalidMarketHours("18:00", "09:00"));
                })));

        assertTrue(error.getMessage().contains("market-hours.open must be before market-hours.close"));
    }

    @Test
    void rejectsBlankCurrencyOnStartupMapping() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> DefaultAssetRegistry.fromProperties(propertiesWith(entry -> entry.setCurrency("  "))));

        assertTrue(error.getMessage().contains("missing required field 'currency'"));
    }

    @Test
    void rejectsZeroSyncIntervalOnStartupMapping() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> DefaultAssetRegistry.fromProperties(propertiesWith(entry -> {
                    AssetRegistryProperties.SyncEntry sync = new AssetRegistryProperties.SyncEntry();
                    sync.setIntervalMinutes(0);
                    sync.setOffsetMinutes(0);
                    entry.setSync(sync);
                })));

        assertTrue(error.getMessage().contains("sync.interval-minutes must be a positive integer"));
    }

    @Test
    void rejectsSyncOffsetGreaterOrEqualIntervalOnStartupMapping() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> DefaultAssetRegistry.fromProperties(propertiesWith(entry -> {
                    AssetRegistryProperties.SyncEntry sync = new AssetRegistryProperties.SyncEntry();
                    sync.setIntervalMinutes(15);
                    sync.setOffsetMinutes(15);
                    entry.setSync(sync);
                })));

        assertTrue(error.getMessage().contains("sync.offset-minutes must be less than sync.interval-minutes"));
    }

    @Test
    void rejectsUnsupportedProviderWithAssetContext() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> DefaultAssetRegistry.fromProperties(propertiesWith(entry -> entry.setProvider("yahoo"))));

        assertTrue(error.getMessage().contains("assets-registry entry 'broken'"));
        assertTrue(error.getMessage().contains("unsupported provider"));
    }

    @Test
    void rejectsDuplicateIdsDifferingOnlyByCase() {
        AssetRegistryProperties properties = new AssetRegistryProperties();
        properties.setEntries(List.of(validEntry(), validEntry(entry -> entry.setId("BROKEN"))));

        IllegalStateException error =
                assertThrows(IllegalStateException.class, () -> DefaultAssetRegistry.fromProperties(properties));

        assertTrue(error.getMessage().contains("duplicate asset id 'broken'"));
    }

    private static AssetRegistryProperties propertiesWith(Consumer<AssetRegistryProperties.AssetEntry> customizer) {
        AssetRegistryProperties properties = new AssetRegistryProperties();
        properties.setEntries(List.of(validEntry(customizer)));
        return properties;
    }

    private static AssetRegistryProperties.AssetEntry validEntry() {
        return validEntry(entry -> {
        });
    }

    private static AssetRegistryProperties.AssetEntry validEntry(
            Consumer<AssetRegistryProperties.AssetEntry> customizer) {
        AssetRegistryProperties.AssetEntry entry = new AssetRegistryProperties.AssetEntry();
        entry.setId("broken");
        entry.setDisplayName("Broken Asset");
        entry.setProvider("fmp");
        entry.setSymbol("BROKEN");
        entry.setDbSymbol("BROKEN");
        entry.setType("STOCK");
        entry.setCurrency("USD");
        entry.setMarketHours(validMarketHours());
        entry.setSync(validSync());
        customizer.accept(entry);
        return entry;
    }

    private static AssetRegistryProperties.MarketHoursEntry validMarketHours() {
        return invalidMarketHours("09:00", "17:00");
    }

    private static AssetRegistryProperties.MarketHoursEntry invalidMarketHours(String open, String close) {
        AssetRegistryProperties.MarketHoursEntry marketHours = new AssetRegistryProperties.MarketHoursEntry();
        marketHours.setZone("Europe/Stockholm");
        marketHours.setOpen(open);
        marketHours.setClose(close);
        return marketHours;
    }

    private static AssetRegistryProperties.SyncEntry validSync() {
        AssetRegistryProperties.SyncEntry sync = new AssetRegistryProperties.SyncEntry();
        sync.setIntervalMinutes(15);
        sync.setOffsetMinutes(0);
        return sync;
    }
}
