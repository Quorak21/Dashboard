package com.dokkcorp.dashboard.providers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.dokkcorp.dashboard.providers.dto.CoinGeckoDto;
import com.dokkcorp.dashboard.providers.dto.CoinGeckoHistoryDto;

@Service
public class CoinGeckoClient {

    private final RestClient restClient;

    public CoinGeckoClient(
            RestClient.Builder builder,
            @Value("${app.coingecko.api-key}") String apiKey,
            @Value("${app.coingecko.base-url}") String baseUrl) {

        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("x-cg-demo-api-key", apiKey)
                .build();
    }

    public CoinGeckoDto[] getData() {

        CoinGeckoDto[] response = this.restClient
                .get()
                .uri("/coins/markets?vs_currency=usd&ids=hyperliquid")
                .retrieve()
                .body(CoinGeckoDto[].class);

        return response;

    }

    public CoinGeckoHistoryDto getHistory() {

        CoinGeckoHistoryDto response = this.restClient
                .get()
                .uri("/coins/hyperliquid/market_chart?vs_currency=usd&days=365&interval=daily")
                .retrieve()
                .body(CoinGeckoHistoryDto.class);

        return response;

    }

}
