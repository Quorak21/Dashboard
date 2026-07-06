package com.dokkcorp.dashboard.features.assets.price;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.dokkcorp.dashboard.exception.ExternalProviderException;
import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetProvider;
import com.dokkcorp.dashboard.providers.fmp.FMPClient;
import com.dokkcorp.dashboard.providers.fmp.FMPDto;

@Service
public class FmpPriceProvider implements PriceProvider {

    private final FMPClient fmpClient;

    public FmpPriceProvider(FMPClient fmpClient) {
        this.fmpClient = fmpClient;
    }

    @Override
    public String providerId() {
        return "fmp";
    }

    @Override
    public PriceQuote fetch(AssetDefinition asset) {
        if (asset.provider() != AssetProvider.FMP) {
            throw new IllegalArgumentException(
                    "FmpPriceProvider cannot fetch asset with provider: " + asset.provider());
        }
        FMPDto[] data = fmpClient.getData(asset.symbol());
        FMPDto raw = data[0];

        if (raw.currency() != null && !raw.currency().equalsIgnoreCase(asset.currency())) {
            boolean isPenceToPound = "GBp".equalsIgnoreCase(raw.currency()) && "GBP".equalsIgnoreCase(asset.currency());
            if (!isPenceToPound) {
                throw new ExternalProviderException("fmp",
                        "Currency mismatch for " + asset.symbol() + ": expected " + asset.currency() + " but got " + raw.currency());
            }
        }

        double price = raw.currentPrice();
        if ("GBp".equals(raw.currency())) {
            price = price / 100.0;
        }
        return new PriceQuote(
                price,
                asset.currency(),
                raw.marketCap(),
                raw.priceChangePercentage24h(),
                raw.totalVolume(),
                Instant.now());
    }
}

