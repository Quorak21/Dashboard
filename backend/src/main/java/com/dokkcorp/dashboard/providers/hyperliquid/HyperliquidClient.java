package com.dokkcorp.dashboard.providers.hyperliquid;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class HyperliquidClient {

        private static final Logger logger = LoggerFactory.getLogger(HyperliquidClient.class);

        private final RestClient restClient;

        public HyperliquidClient(RestClient.Builder builder) {

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

                return new HyperliquidDto(
                                supply != null ? supply.circulatingSupply() : "0",
                                hlp != null ? hlp.totalValueLocked() : "0",
                                hlp != null ? hlp.apr() : "0",
                                volume24H != null ? volume24H.dailyVolume() : "0",
                                openInterest != null ? openInterest.openInterest() : "0",
                                staking != null ? staking.stakingApr() : "0",
                                maxSupply != null ? maxSupply.maxSupply() : "0",
                                maxSupply != null ? maxSupply.hypeBurned() : "0",
                                staking != null ? staking.totalStakedHype() : "0");

        }

        // Supply circulante de HYPE
        private CirculatingSupply fetchTokenData() {

                try {
                        CirculatingSupply response = this.restClient
                                        .post()
                                        .uri("/info")
                                        .header("Content-Type", "application/json")
                                        .body("{ \"type\": \"tokenDetails\", \"tokenId\": \"0x0d01dc56dcaaca66ad901c959b4011ec\" }")
                                        .retrieve()
                                        .body(CirculatingSupply.class);
                        return response;
                } catch (Exception e) {
                        logger.error("Erreur récupération circulating supply HYPE: {}", e.getMessage());
                        return null;
                }
        }

        // La TVL du vault principal + l'APR
        private ProviderHlp fetchProviderData() {

                String apr = "0";
                String totalValueLocked = "0";

                try {
                        JsonNode node = this.restClient
                                        .post()
                                        .uri("/info")
                                        .header("Content-Type", "application/json")
                                        .body("{ \"type\": \"vaultDetails\", \"vaultAddress\": \"0xdfc24b077bc1425ad1dea75bcb6f8158e10df303\" }")
                                        .retrieve()
                                        .body(JsonNode.class);

                        if (node != null) {
                                // Récupération de l'APR
                                if (node.has("apr")) {
                                        apr = node.get("apr").asString();
                                }

                                // Récupération de la TVL
                                JsonNode historyArray = node.get("portfolio").get(0).get(1).get("accountValueHistory");
                                if (historyArray != null && historyArray.isArray()) {
                                        int lastIndex = historyArray.size() - 1;
                                        totalValueLocked = historyArray.get(lastIndex).get(1).asString();
                                }
                        }
                } catch (Exception e) {
                        logger.error("Erreur lors de la récupération des données du vault Hyperliquid : {}",
                                        e.getMessage());
                }

                return new ProviderHlp(totalValueLocked, apr);
        }

        private Volume24H fetchVolume24H() {

                try {
                        return this.restClient
                                        .post()
                                        .uri("/info")
                                        .header("Content-Type", "application/json")
                                        .body("{ \"type\": \"globalStats\"}")
                                        .retrieve()
                                        .body(Volume24H.class);
                } catch (Exception e) {
                        logger.error("Erreur récupération Volume24H: {}", e.getMessage());
                        return null;
                }
        }

        // Open interest total du marché
        private OpenInterest fetchOpenInterest() {

                double openInterest = 0;

                try {
                        JsonNode node = this.restClient
                                        .post()
                                        .uri("/info")
                                        .header("Content-Type", "application/json")
                                        .body("{ \"type\": \"metaAndAssetCtxs\"}")
                                        .retrieve()
                                        .body(JsonNode.class);

                        JsonNode assets = node.get(1);

                        for (int n = 0; n < assets.size(); n++) {
                                JsonNode price = assets.get(n).get("oraclePx");
                                JsonNode openInterestAsset = assets.get(n).get("openInterest");

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
                        JsonNode node = this.restClient
                                        .post()
                                        .uri("/info")
                                        .header("Content-Type", "application/json")
                                        .body("{ \"type\": \"validatorSummaries\"}")
                                        .retrieve()
                                        .body(JsonNode.class);

                        for (int n = 0; n < node.size(); n++) {
                                if (node.get(n).get("name").asString().equals("CMI")) {
                                        stakingApr = Double.toString(
                                                        node.get(n).get("stats").get(2).get(1).get("predictedApr")
                                                                        .asDouble()
                                                                        * 100);
                                }
                        }

                        //Total staked HYPE
                        for (int n = 0; n < node.size(); n++) {

                                if (node.get(n).get("isJailed").asBoolean() == false) {
                                        stakedHype += (node.get(n).get("stake").asDouble() / 100000000);
                                }
                        }

                        totalStakedHype = Double.toString(stakedHype);

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
                        JsonNode node = this.restClient
                                        .post()
                                        .uri("/info")
                                        .header("Content-Type", "application/json")
                                        .body("{ \"type\": \"spotClearinghouseState\", \"user\": \"0xfefefefefefefefefefefefefefefefefefefefe\"}")
                                        .retrieve()
                                        .body(JsonNode.class);

                        JsonNode nodeBalance = node.get("balances");
                        // Boucle sur les différents assets du wallet pour y trouver les hype
                        for (int n = 0; n < nodeBalance.size(); n++) {
                                if (nodeBalance.get(n).get("coin").asString().equals("HYPE")) {
                                        burnedHype = nodeBalance.get(n).get("total").asDouble();
                                        hypeBurned = String.valueOf(burnedHype);
                                        maxSupply = String.valueOf(1000000000 - burnedHype);
                                }
                        }

                        return new MaxSupply(maxSupply, hypeBurned);
                } catch (Exception e) {
                        logger.error("Erreur récupération des HYPE brûlé et la max supply: {}", e.getMessage());
                        return null;
                }
        }

}
