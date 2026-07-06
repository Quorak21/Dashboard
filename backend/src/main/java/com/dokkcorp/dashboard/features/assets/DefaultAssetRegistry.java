package com.dokkcorp.dashboard.features.assets;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetProvider;

public class DefaultAssetRegistry implements AssetRegistry {

    private final List<AssetDefinition> assets;
    private final Map<String, AssetDefinition> assetsById;

    public DefaultAssetRegistry(List<AssetDefinition> assets) {
        this.assets = List.copyOf(assets);
        this.assetsById = assets.stream()
                .collect(Collectors.toUnmodifiableMap(
                        AssetDefinition::id,
                        Function.identity(),
                        (left, right) -> {
                            throw new IllegalStateException(
                                    "assets-registry: duplicate asset id '" + left.id() + "'");
                        }));
    }

    public static DefaultAssetRegistry fromProperties(
            com.dokkcorp.dashboard.config.assets.AssetRegistryProperties properties) {
        return new DefaultAssetRegistry(AssetRegistryMapper.toDefinitions(properties.getEntries()));
    }

    @Override
    public List<AssetDefinition> all() {
        return assets;
    }

    @Override
    public Optional<AssetDefinition> findById(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(assetsById.get(assetId.trim().toLowerCase(Locale.ROOT)));
    }

    @Override
    public List<AssetDefinition> byProvider(AssetProvider provider) {
        return assets.stream().filter(asset -> asset.provider() == provider).toList();
    }
}
