package com.dokkcorp.dashboard.providers.fmp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FMPClient {

        private static final Logger logger = LoggerFactory.getLogger(FMPClient.class);

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

        // Récupération des données selon l'actif voulu et renvoie un tableau
        public FMPDto[] getData(String symbol) {

                try {
                        return this.restClient
                                        .get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/profile")
                                                        .queryParam("symbol", symbol)
                                                        .queryParam("apikey", this.apiKey)
                                                        .build())
                                        .retrieve()
                                        .body(FMPDto[].class);
                } catch (Exception e) {
                        logger.error("Erreur récupération données FMP pour {}: {}", symbol, e.getMessage());

                        // Retourne null pour que le service utilise le cache et éviter d'avoir un 0, on aura juste de la vieille data
                        return null;
                }

        }

}
