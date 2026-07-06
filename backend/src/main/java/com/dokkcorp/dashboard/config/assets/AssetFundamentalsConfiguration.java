package com.dokkcorp.dashboard.config.assets;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

@Configuration
public class AssetFundamentalsConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AssetFundamentalsConfiguration.class);

    @Bean
    public AssetFundamentalsProperties assetFundamentalsProperties() throws IOException {
        AssetFundamentalsProperties properties = new AssetFundamentalsProperties();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        Resource[] resources;
        try {
            resources = resolver.getResources("classpath*:config/fundamentals/*.yml");
            if (resources == null) {
                logger.warn("Scanned fundamentals config: resources array is null");
                return properties;
            }
            logger.info("Scanned fundamentals config: found {} files", resources.length);
        } catch (IOException e) {
            logger.error("Failed to scan fundamentals configurations", e);
            return properties;
        }

        for (Resource resource : resources) {
            if (resource == null || !resource.exists()) {
                continue;
            }
            try {
                YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
                factory.setResources(resource);
                Properties props = factory.getObject();
                if (props != null) {
                    MapConfigurationPropertySource source = new MapConfigurationPropertySource((Map) props);
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

                    Binder binder = new Binder(
                            java.util.Collections.singletonList(source),
                            null,
                            conversionService,
                            null
                    );
                    AssetFundamentalsProperties.FundamentalsConfig config = binder.bind("", Bindable.of(AssetFundamentalsProperties.FundamentalsConfig.class)).orElse(null);
                    if (config != null) {
                        if (!isValid(config)) {
                            logger.warn("Skipped fundamentals config in resource {}: missing mandatory fields (assetId, updatedAt, source, metrics)", resource.getFilename());
                        } else {
                            String key = config.getAssetId().trim().toLowerCase(Locale.ROOT);
                            if (properties.getFundamentals().containsKey(key)) {
                                logger.warn("Duplicate fundamentals configuration for assetId '{}' in file: {}. Overwriting previous entry.", key, resource.getFilename());
                            }
                            properties.getFundamentals().put(key, config);
                            logger.info("Loaded fundamentals config for asset: {} from {}", key, resource.getFilename());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to load fundamentals config from resource: {}", resource.getFilename(), e);
            }
        }
        return properties;
    }

    public static boolean isValid(AssetFundamentalsProperties.FundamentalsConfig config) {
        if (config == null) {
            return false;
        }
        if (config.getAssetId() == null || config.getAssetId().trim().isEmpty() ||
            config.getUpdatedAt() == null ||
            config.getSource() == null || config.getSource().trim().isEmpty() ||
            config.getMetrics() == null || config.getMetrics().isEmpty()) {
            return false;
        }
        if (config.getTopHoldings() != null) {
            for (AssetFundamentalsProperties.HoldingProperties holding : config.getTopHoldings()) {
                if (holding.getWeightPercent() == null || 
                    holding.getWeightPercent().compareTo(java.math.BigDecimal.ZERO) < 0 ||
                    holding.getWeightPercent().compareTo(new java.math.BigDecimal("100")) > 0) {
                    return false;
                }
            }
        }
        if (!areSectorWeightsValid(config.getSectorWeights())) {
            return false;
        }
        if (!areSectorWeightsValid(config.getRetailIndustryWeights())) {
            return false;
        }
        return true;
    }

    private static boolean areSectorWeightsValid(List<AssetFundamentalsProperties.SectorWeightProperties> weights) {
        if (weights == null) {
            return true;
        }
        for (AssetFundamentalsProperties.SectorWeightProperties sector : weights) {
            if (sector.getWeightPercent() == null ||
                sector.getWeightPercent().compareTo(java.math.BigDecimal.ZERO) < 0 ||
                sector.getWeightPercent().compareTo(new java.math.BigDecimal("100")) > 0) {
                return false;
            }
        }
        return true;
    }
}
