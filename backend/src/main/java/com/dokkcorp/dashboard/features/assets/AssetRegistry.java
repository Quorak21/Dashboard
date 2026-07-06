package com.dokkcorp.dashboard.features.assets;

import java.util.List;
import java.util.Optional;

import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetProvider;

public interface AssetRegistry {

    List<AssetDefinition> all();

    Optional<AssetDefinition> findById(String assetId);

    List<AssetDefinition> byProvider(AssetProvider provider);
}
