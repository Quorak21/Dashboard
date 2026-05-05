package com.dokkcorp.dashboard.providers;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import com.dokkcorp.dashboard.providers.dto.HyperliquidDto;

@Service
public class HyperliquidClient {

        private final RestClient restClient;

        public HyperliquidClient(RestClient.Builder builder) {

                this.restClient = builder.baseUrl("https://api.hyperliquid.xyz").build();

        }

        public HyperliquidDto getHlData() {

                CirculatingSupply supply = fetchTokenData();
                ProviderHlp hlp = fetchProviderData();
                Volume24H volume24H = fetchVolume24H();
                OpenInterest openInterest = fetchOpenInterest();
                Staking staking = fetchStaking();
                MaxSupply maxSupply = fetchHypeBurned();

                return new HyperliquidDto(
                                supply.circulatingSupply(),
                                hlp.totalValueLocked(),
                                hlp.apr(),
                                volume24H.dailyVolume(),
                                openInterest.openInterest(),
                                staking.stakingApr(),
                                maxSupply.maxSupply(),
                                maxSupply.hypeBurned(),
                                staking.totalStakedHype());

        }

        private CirculatingSupply fetchTokenData() {
                return this.restClient
                                .post()
                                .uri("/info")
                                .header("Content-Type", "application/json")
                                .body("{ \"type\": \"tokenDetails\", \"tokenId\": \"0x0d01dc56dcaaca66ad901c959b4011ec\" }")
                                .retrieve()
                                .body(CirculatingSupply.class);
        }

        private ProviderHlp fetchProviderData() {

                JsonNode node = this.restClient
                                .post()
                                .uri("/info")
                                .header("Content-Type", "application/json")
                                .body("{ \"type\": \"vaultDetails\", \"vaultAddress\": \"0xdfc24b077bc1425ad1dea75bcb6f8158e10df303\" }")
                                .retrieve()
                                .body(JsonNode.class);

                String apr = node.get("apr").asString();

                JsonNode historyArray = node
                                .get("portfolio")
                                .get(0)
                                .get(1)
                                .get("accountValueHistory");

                int lastIndex = historyArray.size() - 1;
                String totalValueLocked = historyArray.get(lastIndex).get(1).asString();

                return new ProviderHlp(totalValueLocked, apr);
        }

        private Volume24H fetchVolume24H() {
                return this.restClient
                                .post()
                                .uri("/info")
                                .header("Content-Type", "application/json")
                                .body("{ \"type\": \"globalStats\"}")
                                .retrieve()
                                .body(Volume24H.class);
        }

        private OpenInterest fetchOpenInterest() {

                double openInterest = 0;

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
        }

        private Staking fetchStaking() {

                String stakingApr = "0";
                double stakedHype = 0;
                String totalStakedHype = "0";

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
                                                node.get(n).get("stats").get(2).get(1).get("predictedApr").asDouble()
                                                                * 100);
                        }
                }

                for (int n = 0; n < node.size(); n++) {

                        if (node.get(n).get("isJailed").asBoolean() == false) {
                                stakedHype += (node.get(n).get("stake").asDouble() / 100000000);
                        }
                }

                totalStakedHype = Double.toString(stakedHype);

                return new Staking(stakingApr, totalStakedHype);
        }

        private MaxSupply fetchHypeBurned() {

                double burnedHype = 0;
                String maxSupply = "";
                String hypeBurned = "";

                JsonNode node = this.restClient
                                .post()
                                .uri("/info")
                                .header("Content-Type", "application/json")
                                .body("{ \"type\": \"spotClearinghouseState\", \"user\": \"0xfefefefefefefefefefefefefefefefefefefefe\"}")
                                .retrieve()
                                .body(JsonNode.class);

                JsonNode nodeBalance = node.get("balances");

                for (int n = 0; n < nodeBalance.size(); n++) {
                        if (nodeBalance.get(n).get("coin").asString().equals("HYPE")) {
                                burnedHype = nodeBalance.get(n).get("total").asDouble();
                                hypeBurned = String.valueOf(burnedHype);
                                maxSupply = String.valueOf(1000000000 - burnedHype);
                        }
                }

                return new MaxSupply(maxSupply, hypeBurned);
        }

        record CirculatingSupply(
                        String circulatingSupply) {
        }

        record ProviderHlp(
                        String totalValueLocked,
                        String apr) {
        }

        record Volume24H(
                        String dailyVolume) {
        }

        record OpenInterest(
                        String openInterest) {
        }

        record Staking(
                        String stakingApr,
                        String totalStakedHype) {
        }

        record MaxSupply(
                        String maxSupply,
                        String hypeBurned) {
        }

}
