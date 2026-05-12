package com.dokkcorp.dashboard.providers.stocks;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

@Service
public class ForexClient {

    private final RestClient restClient;

    public ForexClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public Double getSekUsdratio() {
        try {
            JsonNode response = this.restClient.get()
                    .uri("https://api.frankfurter.dev/v1/latest?from=SEK&to=USD")
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode responseRatio = response.get("rates").get("USD");
            return responseRatio.asDouble();

        } catch (Exception e) {
            return 1.0;
        }
    }

    public Double getSekChfRatio() {
        try {
            JsonNode response = this.restClient.get()
                    .uri("https://api.frankfurter.dev/v1/latest?from=SEK&to=CHF")
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode responseRatio = response.get("rates").get("CHF");
            return responseRatio.asDouble();

        } catch (Exception e) {
            return 1.0;
        }
    }
}
