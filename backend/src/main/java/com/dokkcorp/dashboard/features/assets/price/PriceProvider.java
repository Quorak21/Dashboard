package com.dokkcorp.dashboard.features.assets.price;

import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;

public interface PriceProvider {

    String providerId();

    PriceQuote fetch(AssetDefinition asset);
}
