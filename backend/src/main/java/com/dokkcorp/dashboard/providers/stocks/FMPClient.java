package com.dokkcorp.dashboard.providers.stocks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.dokkcorp.dashboard.providers.dto.stocks.FMPDto;

@Service
public class FMPClient {

        private final RestClient restClient;
        private final String apiKey;

        public FMPClient(
                        RestClient.Builder builder,
                        @Value("${app.fmp.api-key}") String apiKey,
                        @Value("${app.fmp.base-url}") String baseUrl) {

                this.apiKey = apiKey;
                this.restClient = builder
                                .baseUrl(baseUrl)
                                .build();
        }

        public FMPDto[] getData(String symbol) {

                FMPDto[] response = this.restClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/profile")
                                                .queryParam("symbol", symbol)
                                                .queryParam("apikey", this.apiKey)
                                                .build())
                                .retrieve()
                                .body(FMPDto[].class);

                return response;

        }

}
