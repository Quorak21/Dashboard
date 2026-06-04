package com.dokkcorp.dashboard.providers.fmp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.dokkcorp.dashboard.config.ExternalCallExecutor;
import com.dokkcorp.dashboard.exception.ExternalProviderException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FMPClient {

        private static final Logger logger = LoggerFactory.getLogger(FMPClient.class);
        private static final String PROVIDER = "fmp";

        private final RestClient restClient;
        private final ExternalCallExecutor externalCallExecutor;
        private final String apiKey;

        public FMPClient(
                        RestClient.Builder builder,
                        ExternalCallExecutor externalCallExecutor,
                        @Value("${app.fmp.api-key}") String apiKey,
                        @Value("${app.fmp.base-url}") String baseUrl) {

                this.apiKey = apiKey;
                this.externalCallExecutor = externalCallExecutor;
                this.restClient = builder
                                .baseUrl(baseUrl)
                                .build();
        }

        public FMPDto[] getData(String symbol) {
                try {
                        FMPDto[] result = externalCallExecutor.execute(() -> this.restClient
                                        .get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/profile")
                                                        .queryParam("symbol", symbol)
                                                        .queryParam("apikey", this.apiKey)
                                                        .build())
                                        .retrieve()
                                        .body(FMPDto[].class));
                        if (result == null || result.length == 0) {
                                throw new ExternalProviderException(PROVIDER,
                                                "Réponse vide pour le symbole " + symbol);
                        }
                        return result;
                } catch (ExternalProviderException e) {
                        throw e;
                } catch (Exception e) {
                        logger.error("Erreur récupération données FMP pour {}: {}", symbol, e.getMessage());
                        throw new ExternalProviderException(PROVIDER,
                                        "Échec getData pour " + symbol, e);
                }
        }

}
