package com.dokkcorp.dashboard.config.assets;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dokkcorp.dashboard.features.assets.AssetRegistry;
import com.dokkcorp.dashboard.features.assets.DefaultAssetRegistry;

@Configuration
@EnableConfigurationProperties(AssetRegistryProperties.class)
public class AssetRegistryConfiguration {

    @Bean
    AssetRegistry assetRegistry(AssetRegistryProperties properties) {
        return DefaultAssetRegistry.fromProperties(properties);
    }
}
