package com.dokkcorp.dashboard.config.assets;

import java.io.IOException;
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
public class AssetDividendsConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AssetDividendsConfiguration.class);

    @Bean
    public AssetDividendsProperties assetDividendsProperties() throws IOException {
        AssetDividendsProperties properties = new AssetDividendsProperties();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        
        Resource[] resources;
        try {
            resources = resolver.getResources("classpath*:config/dividends/*.yml");
            if (resources == null) {
                logger.warn("Scanned dividends config: resources array is null");
                return properties;
            }
            logger.info("Scanned dividends config: found {} files", resources.length);
        } catch (IOException e) {
            logger.error("Failed to scan dividends configurations", e);
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
                    Binder binder = new Binder(source);
                    AssetDividendsProperties.DividendsConfig config = binder.bind("", Bindable.of(AssetDividendsProperties.DividendsConfig.class)).orElse(null);
                    if (config != null) {
                        if (config.getAssetId() == null || config.getAssetId().trim().isEmpty() ||
                            config.getForwardDividend() == null ||
                            config.getForwardDividendCurrency() == null || config.getForwardDividendCurrency().trim().isEmpty() ||
                            config.getFrequency() == null || config.getFrequency().trim().isEmpty()) {
                            logger.warn("Skipped dividend config in resource {}: missing or empty mandatory fields (assetId, forwardDividend, forwardDividendCurrency, frequency)", resource.getFilename());
                        } else {
                            String key = config.getAssetId().trim().toLowerCase(Locale.ROOT);
                            properties.getDividends().put(key, config);
                            logger.info("Loaded dividend config for asset: {} from {}", key, resource.getFilename());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to load dividend config from resource: {}", resource.getFilename(), e);
            }
        }
        return properties;
    }
}
