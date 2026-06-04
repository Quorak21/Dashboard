package com.dokkcorp.dashboard.providers.hyperliquid;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.dokkcorp.dashboard.config.ExternalCallExecutor;
import com.dokkcorp.dashboard.features.crypto.hype.maths.HypeConstants;

import tools.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class HyperliquidClient {

        private static final Logger logger = LoggerFactory.getLogger(HyperliquidClient.class);

        private final RestClient restClient;
        private final ExternalCallExecutor externalCallExecutor;

        public HyperliquidClient(RestClient.Builder builder, ExternalCallExecutor externalCallExecutor) {

                this.externalCallExecutor = externalCallExecutor;
                this.restClient = builder.baseUrl("https://api.hyperliquid.xyz").build();

        }

        // Data globale du protocole via API hyperliquid
        public HyperliquidDto getHlData() {

                // Les mini DTO pour récup plusieurs data en un seul appel ont été divisé en plusieur class dans le meme dossier
                // Permet aussi de déserialiser directement le JSON dans un objet avec Jackson
                CirculatingSupply supply = fetchTokenData();
                ProviderHlp hlp = fetchProviderData();
                Volume24H volume24H = fetchVolume24H();
                OpenInterest openInterest = fetchOpenInterest();
                Staking staking = fetchStaking();
                MaxSupply maxSupply = fetchHypeBurned();

                return new HyperliquidDto(supply != null ? supply.circulatingSupply() : "0", hlp != null ? hlp.providerTvl() : "0", hlp != null ? hlp.providerApr() : "0",
                                volume24H != null ? volume24H.dailyVolume() : "0", openInterest != null ? openInterest.openInterest() : "0", staking != null ? staking.stakingApr() : "0",
                                maxSupply != null ? maxSupply.maxSupply() : "0", maxSupply != null ? maxSupply.hypeBurned() : "0", staking != null ? staking.totalStakedHype() : "0");

        }

        // Supply circulante de HYPE
        private CirculatingSupply fetchTokenData() {

                try {
                        CirculatingSupply response = externalCallExecutor.execute(() -> this.restClient.post().uri("/info").header("Content-Type", "application/json")
                                        .body("{ \"type\": \"tokenDetails\", \"tokenId\": \"0x0d01dc56dcaaca66ad901c959b4011ec\" }").retrieve().body(CirculatingSupply.class));
                        return response;
                } catch (Exception e) {
                        logger.error("Erreur récupération circulating supply HYPE: {}", e.getMessage());
                        return null;
                }
        }

        // La TVL du vault principal + l'APR
        private ProviderHlp fetchProviderData() {

                String providerApr = "0";
                String providerTvl = "0";

                try {
                        JsonNode node = externalCallExecutor.execute(() -> this.restClient.post().uri("/info").header("Content-Type", "application/json")
                                        .body("{ \"type\": \"vaultDetails\", \"vaultAddress\": \"0xdfc24b077bc1425ad1dea75bcb6f8158e10df303\" }").retrieve().body(JsonNode.class));

                        if (node != null) {
                                // Récupération de l'APR
                                if (node.has("apr")) {
                                        providerApr = node.get("apr").asString();
                                }

                                // Récupération de la TVL
                                JsonNode historyArray = node.path("portfolio").path(0).path(1).path("accountValueHistory");
                                if (historyArray != null && historyArray.isArray()) {
                                        int lastIndex = historyArray.size() - 1;
                                        if (lastIndex >= 0) {
                                                providerTvl = safeStringValue(historyArray.path(lastIndex).path(1), "0");
                                        }
                                }
                        }
                } catch (Exception e) {
                        logger.error("Erreur lors de la récupération des données du vault Hyperliquid : {}", e.getMessage());
                }

                return new ProviderHlp(providerTvl, providerApr);
        }

        private Volume24H fetchVolume24H() {

                try {
                        return externalCallExecutor.execute(() -> this.restClient.post().uri("/info").header("Content-Type", "application/json").body("{ \"type\": \"globalStats\"}").retrieve().body(Volume24H.class));
                } catch (Exception e) {
                        logger.error("Erreur récupération Volume24H: {}", e.getMessage());
                        return null;
                }
        }

        // Open interest total du marché
        private OpenInterest fetchOpenInterest() {

                double openInterest = 0;

                try {
                        JsonNode node = externalCallExecutor.execute(() -> this.restClient.post().uri("/info").header("Content-Type", "application/json").body("{ \"type\": \"metaAndAssetCtxs\"}").retrieve().body(JsonNode.class));

                        JsonNode assets = node != null ? node.path(1) : null;
                        if (assets == null || !assets.isArray()) {
                                logger.warn("Format inattendu pour open interest: assets manquant ou invalide");
                                return new OpenInterest("0");
                        }

                        for (int n = 0; n < assets.size(); n++) {
                                JsonNode price = assets.path(n).path("oraclePx");
                                JsonNode openInterestAsset = assets.path(n).path("openInterest");

                                double openinterestDollar = price.asDouble() * openInterestAsset.asDouble();
                                openInterest += openinterestDollar;
                        }

                        String totalOpenInterest = Double.toString(openInterest);

                        return new OpenInterest(totalOpenInterest);
                } catch (Exception e) {
                        logger.error("Erreur récupération open interest: {}", e.getMessage());
                        return null;
                }
        }

        // Apr moyen du staking
        private Staking fetchStaking() {

                String stakingApr = "0";
                double stakedHype = 0;
                String totalStakedHype = "0";

                //APR
                try {
                        JsonNode node = externalCallExecutor.execute(() -> this.restClient.post().uri("/info").header("Content-Type", "application/json").body("{ \"type\": \"validatorSummaries\"}").retrieve().body(JsonNode.class));

                        if (node == null || !node.isArray()) {
                                logger.warn("Format inattendu pour staking: node manquant ou invalide");
                                return new Staking(stakingApr, totalStakedHype);
                        }

                        for (int n = 0; n < node.size(); n++) {
                                JsonNode validator = node.path(n);
                                if ("CMI".equals(safeStringValue(validator.path("name"), ""))) {
                                        stakingApr = Double.toString(validator.path("stats").path(2).path(1).path("predictedApr").asDouble() * 100);
                                }
                        }

                        // Total staked HYPE (normalized from raw 8-decimal units)
                        for (int n = 0; n < node.size(); n++) {

                                JsonNode validator = node.path(n);
                                if (!validator.path("isJailed").asBoolean(false)) {
                                        double stakeRaw = validator.path("stake").asDouble(0d);
                                        stakedHype += stakeRaw / HypeConstants.STAKE_SCALE_FACTOR;
                                }
                        }

                        totalStakedHype = String.valueOf(stakedHype);

                        return new Staking(stakingApr, totalStakedHype);
                } catch (Exception e) {
                        logger.error("Erreur récupération de l'APR moyen et du total staked HYPE: {}", e.getMessage());
                        return null;
                }
        }

        // Récupération de la Max Supply (Total - burned) et du nombre de HYPE brûlé
        private MaxSupply fetchHypeBurned() {

                double burnedHype = 0;
                String maxSupply = "";
                String hypeBurned = "";

                try {
                        JsonNode node = externalCallExecutor.execute(() -> this.restClient.post().uri("/info").header("Content-Type", "application/json")
                                        .body("{ \"type\": \"spotClearinghouseState\", \"user\": \"0xfefefefefefefefefefefefefefefefefefefefe\"}").retrieve().body(JsonNode.class));

                        JsonNode nodeBalance = node != null ? node.path("balances") : null;
                        if (nodeBalance == null || !nodeBalance.isArray()) {
                                logger.warn("Format inattendu pour burned HYPE: balances manquant ou invalide");
                                return new MaxSupply("0", "0");
                        }
                        // Boucle sur les différents assets du wallet pour y trouver les hype
                        for (int n = 0; n < nodeBalance.size(); n++) {
                                JsonNode balance = nodeBalance.path(n);
                                if ("HYPE".equals(safeStringValue(balance.path("coin"), ""))) {
                                        burnedHype = balance.path("total").asDouble(0d);
                                        hypeBurned = String.valueOf(burnedHype);
                                        maxSupply = String.valueOf(HypeConstants.TOTAL_SUPPLY - burnedHype);
                                }
                        }

                        return new MaxSupply(maxSupply, hypeBurned);
                } catch (Exception e) {
                        logger.error("Erreur récupération des HYPE brûlé et la max supply: {}", e.getMessage());
                        return null;
                }
        }

        private String safeStringValue(JsonNode node, String defaultValue) {
                if (node == null) {
                        return defaultValue;
                }
                try {
                        String value = node.asString();
                        if (value == null || value.isBlank()) {
                                return defaultValue;
                        }
                        return value;
                } catch (Exception e) {
                        return defaultValue;
                }
        }

}
