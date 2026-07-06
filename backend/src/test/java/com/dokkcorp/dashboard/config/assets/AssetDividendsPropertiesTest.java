package com.dokkcorp.dashboard.config.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class AssetDividendsPropertiesTest {

    @Test
    void testDividendsConfigLoading() throws IOException {
        AssetDividendsConfiguration configuration = new AssetDividendsConfiguration();
        AssetDividendsProperties properties = configuration.assetDividendsProperties();

        assertNotNull(properties);
        assertNotNull(properties.getDividends());
        
        // inveb should be loaded because inveb.yml is in resources/config/dividends/
        assertTrue(properties.getDividends().containsKey("inveb"));
        
        AssetDividendsProperties.DividendsConfig invebConfig = properties.getDividends().get("inveb");
        assertNotNull(invebConfig);
        assertEquals("inveb", invebConfig.getAssetId());
        assertEquals(0, new BigDecimal("6.00").compareTo(invebConfig.getForwardDividend()));
        assertEquals("SEK", invebConfig.getForwardDividendCurrency());
        assertEquals("annual", invebConfig.getFrequency());
        assertEquals(0, new BigDecimal("8.2").compareTo(invebConfig.getAvgDividendGrowth10Y()));
        
        List<AssetDividendsProperties.DividendHistoryEntry> history = invebConfig.getHistory();
        assertNotNull(history);
        assertEquals(3, history.size());
        
        assertEquals(2024, history.get(0).getYear());
        assertEquals(0, new BigDecimal("6.00").compareTo(history.get(0).getAmount()));
        assertEquals("SEK", history.get(0).getCurrency());
        
        assertEquals(2023, history.get(1).getYear());
        assertEquals(0, new BigDecimal("5.50").compareTo(history.get(1).getAmount()));
        assertEquals("SEK", history.get(1).getCurrency());

        assertEquals(2022, history.get(2).getYear());
        assertEquals(0, new BigDecimal("4.00").compareTo(history.get(2).getAmount()));
        assertEquals("SEK", history.get(2).getCurrency());
    }

    @Test
    void testMissingOptionalDataBinding() {
        String yaml = "asset-id: test-asset\n"
                    + "forward-dividend: 3.50\n"
                    + "forward-dividend-currency: USD\n"
                    + "frequency: quarterly\n"; // missing avg-dividend-growth-10y and history
        
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(yaml.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "test-asset.yml";
            }
        };
        
        org.springframework.beans.factory.config.YamlPropertiesFactoryBean factory = new org.springframework.beans.factory.config.YamlPropertiesFactoryBean();
        factory.setResources(resource);
        java.util.Properties props = factory.getObject();
        assertNotNull(props);
        
        org.springframework.boot.context.properties.bind.Binder binder = new org.springframework.boot.context.properties.bind.Binder(
                new org.springframework.boot.context.properties.source.MapConfigurationPropertySource((java.util.Map) props));
        AssetDividendsProperties.DividendsConfig config = binder.bind("", org.springframework.boot.context.properties.bind.Bindable.of(AssetDividendsProperties.DividendsConfig.class)).orElse(null);
        
        assertNotNull(config);
        assertEquals("test-asset", config.getAssetId());
        assertEquals(0, new java.math.BigDecimal("3.50").compareTo(config.getForwardDividend()));
        assertEquals("USD", config.getForwardDividendCurrency());
        assertEquals("quarterly", config.getFrequency());
        assertNull(config.getAvgDividendGrowth10Y());
        assertTrue(config.getHistory().isEmpty());
    }

    @Test
    void testMissingMandatoryFieldsBinding() {
        String yaml = "asset-id: test-asset\n"
                    + "forward-dividend: 3.50\n"; // missing currency and frequency
        
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(yaml.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "test-invalid.yml";
            }
        };
        
        org.springframework.beans.factory.config.YamlPropertiesFactoryBean factory = new org.springframework.beans.factory.config.YamlPropertiesFactoryBean();
        factory.setResources(resource);
        java.util.Properties props = factory.getObject();
        assertNotNull(props);
        
        org.springframework.boot.context.properties.bind.Binder binder = new org.springframework.boot.context.properties.bind.Binder(
                new org.springframework.boot.context.properties.source.MapConfigurationPropertySource((java.util.Map) props));
        AssetDividendsProperties.DividendsConfig config = binder.bind("", org.springframework.boot.context.properties.bind.Bindable.of(AssetDividendsProperties.DividendsConfig.class)).orElse(null);
        
        assertNotNull(config);
        assertEquals("test-asset", config.getAssetId());
        assertNull(config.getForwardDividendCurrency());
        assertNull(config.getFrequency());
    }
}
