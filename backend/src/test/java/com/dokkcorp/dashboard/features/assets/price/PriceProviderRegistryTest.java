package com.dokkcorp.dashboard.features.assets.price;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;

class PriceProviderRegistryTest {

    @Test
    void findById_resolvesProviderCaseInsensitive() {
        PriceProvider fmp = new StubProvider("fmp");
        PriceProviderRegistry registry = new PriceProviderRegistry(List.of(fmp));

        assertTrue(registry.findById("fmp").isPresent());
        assertTrue(registry.findById("FMP").isPresent());
        assertEquals(fmp, registry.findById("Fmp").orElseThrow());
    }

    @Test
    void findById_returnsEmptyForUnknownProvider() {
        PriceProviderRegistry registry = new PriceProviderRegistry(List.of(new StubProvider("fmp")));

        assertTrue(registry.findById("scrape").isEmpty());
        assertTrue(registry.findById("").isEmpty());
        assertTrue(registry.findById(null).isEmpty());
    }

    @Test
    void requireById_throwsForUnknownProvider() {
        PriceProviderRegistry registry = new PriceProviderRegistry(List.of(new StubProvider("fmp")));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> registry.requireById("scrape"));

        assertEquals("Unknown price provider: scrape", error.getMessage());
    }

    @Test
    void rejectsDuplicateProviderIds() {
        assertThrows(
                IllegalStateException.class,
                () -> new PriceProviderRegistry(List.of(
                        new StubProvider("fmp"),
                        new StubProvider("fmp"))));
    }

    private static final class StubProvider implements PriceProvider {

        private final String id;

        private StubProvider(String id) {
            this.id = id;
        }

        @Override
        public String providerId() {
            return id;
        }

        @Override
        public PriceQuote fetch(AssetDefinition asset) {
            return new PriceQuote(0d, "USD", null, null, null, Instant.now());
        }
    }
}
