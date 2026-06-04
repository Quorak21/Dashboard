package com.dokkcorp.dashboard.providers.coingecko;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.dokkcorp.dashboard.config.ExternalCallExecutor;
import com.dokkcorp.dashboard.exception.ExternalProviderException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CoinGeckoClient {

    private static final Logger logger = LoggerFactory.getLogger(CoinGeckoClient.class);
    private static final String PROVIDER = "coingecko";

    private final RestClient restClient;
    private final ExternalCallExecutor externalCallExecutor;

    public CoinGeckoClient(
            RestClient.Builder builder,
            ExternalCallExecutor externalCallExecutor,
            @Value("${app.coingecko.api-key}") String apiKey,
            @Value("${app.coingecko.base-url}") String baseUrl) {

        this.externalCallExecutor = externalCallExecutor;
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("x-cg-demo-api-key", apiKey)
                .build();
    }

    // Data classique du marché (prix courant, market cap, volume, etc..)
    public CoinGeckoDto[] getData() {
        try {
            CoinGeckoDto[] result = externalCallExecutor.execute(() -> this.restClient
                    .get()
                    .uri("/coins/markets?vs_currency=usd&ids=hyperliquid")
                    .retrieve()
                    .body(CoinGeckoDto[].class));
            if (result == null || result.length == 0) {
                throw new ExternalProviderException(PROVIDER, "Réponse vide pour getData");
            }
            return result;
        } catch (ExternalProviderException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching data from CoinGecko: {}", e.getMessage());
            throw new ExternalProviderException(PROVIDER, "Échec getData", e);
        }
    }

    // Data pour le graph annuel, 1j = 1prix
    public CoinGeckoHistoryDto getHistory() {
        try {
            CoinGeckoHistoryDto result = externalCallExecutor.execute(() -> this.restClient
                    .get()
                    .uri("/coins/hyperliquid/market_chart?vs_currency=usd&days=365&interval=daily")
                    .retrieve()
                    .body(CoinGeckoHistoryDto.class));
            if (result == null || result.prices() == null || result.prices().isEmpty()) {
                throw new ExternalProviderException(PROVIDER, "Réponse vide pour getHistory");
            }
            return result;
        } catch (ExternalProviderException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching historical data from CoinGecko: {}", e.getMessage());
            throw new ExternalProviderException(PROVIDER, "Échec getHistory", e);
        }
    }

}
