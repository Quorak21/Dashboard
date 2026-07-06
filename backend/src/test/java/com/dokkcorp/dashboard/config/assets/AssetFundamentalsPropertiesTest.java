package com.dokkcorp.dashboard.config.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class AssetFundamentalsPropertiesTest {

    @Test
    void testFundamentalsConfigLoading() throws IOException {
        AssetFundamentalsConfiguration configuration = new AssetFundamentalsConfiguration();
        AssetFundamentalsProperties properties = configuration.assetFundamentalsProperties();

        assertNotNull(properties);
        assertNotNull(properties.getFundamentals());

        // inveb should be loaded because inveb.yml is in resources/config/fundamentals/
        assertTrue(properties.getFundamentals().containsKey("inveb"));

        AssetFundamentalsProperties.FundamentalsConfig invebConfig = properties.getFundamentals().get("inveb");
        assertNotNull(invebConfig);
        assertEquals("inveb", invebConfig.getAssetId());
        assertEquals(LocalDate.of(2026, 6, 30), invebConfig.getUpdatedAt());
        assertEquals("Investor AB — Annual Report 2025 & Q1 2026", invebConfig.getSource());
        assertNotNull(invebConfig.getMetrics());
        assertEquals(6.11, invebConfig.getMetrics().get("trailing-pe"));
        assertEquals("1.2%", invebConfig.getMetrics().get("debt-leverage"));

        List<AssetFundamentalsProperties.HoldingProperties> topHoldings = invebConfig.getTopHoldings();
        assertNotNull(topHoldings);
        assertEquals(10, topHoldings.size());
        assertEquals("ABB", topHoldings.get(0).getName());
        assertEquals(0, new BigDecimal("16.5").compareTo(topHoldings.get(0).getWeightPercent()));

        // brwm should be loaded
        assertTrue(properties.getFundamentals().containsKey("brwm"));
        AssetFundamentalsProperties.FundamentalsConfig brwmConfig = properties.getFundamentals().get("brwm");
        assertNotNull(brwmConfig);
        assertEquals("brwm", brwmConfig.getAssetId());
        assertEquals(LocalDate.of(2026, 6, 30), brwmConfig.getUpdatedAt());
        assertEquals(
                "BlackRock World Mining Trust — Portfolio Update Q2 2026 (30 June 2026)",
                brwmConfig.getSource());
        assertEquals("~5.5%", brwmConfig.getMetrics().get("five-y-avg-discount"));
        assertEquals("6.9%", brwmConfig.getMetrics().get("gearing-leverage"));
        assertEquals("0.95%", brwmConfig.getMetrics().get("ongoing-charges-ter"));
        assertFalse(brwmConfig.getTopHoldings().isEmpty());
        assertEquals(10, brwmConfig.getTopHoldings().size());
        assertEquals("Glencore", brwmConfig.getTopHoldings().get(0).getName());
        assertEquals(0, new BigDecimal("7.6").compareTo(brwmConfig.getTopHoldings().get(0).getWeightPercent()));

        // o (Realty Income) — property types + retail tenant industries
        assertTrue(properties.getFundamentals().containsKey("o"));
        AssetFundamentalsProperties.FundamentalsConfig oConfig = properties.getFundamentals().get("o");
        assertNotNull(oConfig);
        assertEquals(4, oConfig.getSectorWeights().size());
        assertEquals("Retail", oConfig.getSectorWeights().get(0).getSector());
        assertEquals(0, new BigDecimal("78.9").compareTo(oConfig.getSectorWeights().get(0).getWeightPercent()));
        assertFalse(oConfig.getRetailIndustryWeights().isEmpty());
        assertEquals("Grocery", oConfig.getRetailIndustryWeights().get(0).getSector());
        assertEquals(0, new BigDecimal("11.0").compareTo(oConfig.getRetailIndustryWeights().get(0).getWeightPercent()));
    }

    @Test
    void testMissingOptionalDataBinding() {
        String yaml = "asset-id: test-asset\n"
                    + "updated-at: 2026-06-24\n"
                    + "source: \"Test Source\"\n"
                    + "metrics:\n"
                    + "  nav-per-share: 100.0\n"; // missing top-holdings and sector-weights

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

        org.springframework.format.support.DefaultFormattingConversionService conversionService = 
                new org.springframework.format.support.DefaultFormattingConversionService();
        conversionService.addConverter(String.class, java.time.LocalDate.class, sourceString -> {
            try {
                return java.time.LocalDate.parse(sourceString);
            } catch (Exception e) {
                try {
                    java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", java.util.Locale.ENGLISH);
                    return format.parse(sourceString).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Failed to parse date: " + sourceString, ex);
                }
            }
        });
        conversionService.addConverter(java.util.Date.class, java.time.LocalDate.class, sourceDate -> 
            sourceDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        );

        org.springframework.boot.context.properties.bind.Binder binder = new org.springframework.boot.context.properties.bind.Binder(
                java.util.Collections.singletonList(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource((java.util.Map<?, ?>) props)),
                null,
                conversionService,
                null
        );
        AssetFundamentalsProperties.FundamentalsConfig config = binder.bind("", org.springframework.boot.context.properties.bind.Bindable.of(AssetFundamentalsProperties.FundamentalsConfig.class)).orElse(null);

        assertNotNull(config);
        assertEquals("test-asset", config.getAssetId());
        assertEquals(LocalDate.of(2026, 6, 24), config.getUpdatedAt());
        assertEquals("Test Source", config.getSource());
        assertEquals(100.0, config.getMetrics().get("nav-per-share"));
        assertTrue(config.getTopHoldings().isEmpty());
        assertTrue(config.getSectorWeights().isEmpty());
        assertTrue(config.getRetailIndustryWeights().isEmpty());
        assertTrue(AssetFundamentalsConfiguration.isValid(config));
    }

    @Test
    void testMissingMandatoryFieldsBinding() {
        String yaml = "asset-id: test-asset\n"
                    + "source: \"Test Source\"\n"; // missing updatedAt and metrics

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

        org.springframework.format.support.DefaultFormattingConversionService conversionService = 
                new org.springframework.format.support.DefaultFormattingConversionService();
        conversionService.addConverter(String.class, java.time.LocalDate.class, sourceString -> {
            try {
                return java.time.LocalDate.parse(sourceString);
            } catch (Exception e) {
                try {
                    java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", java.util.Locale.ENGLISH);
                    return format.parse(sourceString).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Failed to parse date: " + sourceString, ex);
                }
            }
        });
        conversionService.addConverter(java.util.Date.class, java.time.LocalDate.class, sourceDate -> 
            sourceDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        );

        org.springframework.boot.context.properties.bind.Binder binder = new org.springframework.boot.context.properties.bind.Binder(
                java.util.Collections.singletonList(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource((java.util.Map<?, ?>) props)),
                null,
                conversionService,
                null
        );
        AssetFundamentalsProperties.FundamentalsConfig config = binder.bind("", org.springframework.boot.context.properties.bind.Bindable.of(AssetFundamentalsProperties.FundamentalsConfig.class)).orElse(null);

        assertNotNull(config);
        assertEquals("test-asset", config.getAssetId());
        assertNull(config.getUpdatedAt());
        assertTrue(config.getMetrics().isEmpty());
        assertFalse(AssetFundamentalsConfiguration.isValid(config));
    }

    @Test
    void testIsValidValidationLogic() {
        AssetFundamentalsProperties.FundamentalsConfig config = new AssetFundamentalsProperties.FundamentalsConfig();
        // Null config
        assertFalse(AssetFundamentalsConfiguration.isValid(null));

        // Missing all fields
        assertFalse(AssetFundamentalsConfiguration.isValid(config));

        // Populate fields except metrics
        config.setAssetId("test");
        config.setUpdatedAt(LocalDate.now());
        config.setSource("Source");
        assertFalse(AssetFundamentalsConfiguration.isValid(config));

        // Metrics empty
        config.setMetrics(new java.util.HashMap<>());
        assertFalse(AssetFundamentalsConfiguration.isValid(config));

        // Metrics populated -> Valid
        config.getMetrics().put("key", "value");
        assertTrue(AssetFundamentalsConfiguration.isValid(config));
    }
}
