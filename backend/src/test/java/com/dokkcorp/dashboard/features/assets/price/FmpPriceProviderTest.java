package com.dokkcorp.dashboard.features.assets.price;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import com.dokkcorp.dashboard.config.ExternalCallExecutor;
import com.dokkcorp.dashboard.exception.ExternalProviderException;
import com.dokkcorp.dashboard.features.assets.model.AssetDefinition;
import com.dokkcorp.dashboard.features.assets.model.AssetProvider;
import com.dokkcorp.dashboard.features.assets.model.AssetType;
import com.dokkcorp.dashboard.features.assets.model.MarketHours;
import com.dokkcorp.dashboard.features.assets.model.SyncConfig;
import com.dokkcorp.dashboard.providers.fmp.FMPClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

class FmpPriceProviderTest {

    private static WireMockServer wireMock;
    private static FmpPriceProvider provider;

    private static final AssetDefinition inveb = new AssetDefinition(
            "inveb",
            "Investor AB",
            AssetProvider.FMP,
            "INVE-B.ST",
            "INVE-B",
            AssetType.STOCK,
            "SEK",
            new MarketHours(ZoneId.of("Europe/Stockholm"), LocalTime.of(9, 0), LocalTime.of(17, 35)),
            new SyncConfig(15, 0),
            null);

    private static final AssetDefinition brwm = new AssetDefinition(
            "brwm",
            "BlackRock World Mining",
            AssetProvider.FMP,
            "BRWM.L",
            "BRWM",
            AssetType.TRUST,
            "GBP",
            new MarketHours(ZoneId.of("Europe/London"), LocalTime.of(8, 0), LocalTime.of(16, 35)),
            new SyncConfig(15, 0),
            null);

    private static final AssetDefinition o = new AssetDefinition(
            "o",
            "Realty Income",
            AssetProvider.FMP,
            "O",
            "O",
            AssetType.REIT,
            "USD",
            new MarketHours(ZoneId.of("America/New_York"), LocalTime.of(9, 30), LocalTime.of(16, 0)),
            new SyncConfig(15, 0),
            null);

    @BeforeAll
    static void beforeAll() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        ExternalCallExecutor executor = new ExternalCallExecutor(1, 0);
        FMPClient fmpClient = new FMPClient(
                RestClient.builder(),
                executor,
                "test-key",
                wireMock.baseUrl());
        provider = new FmpPriceProvider(fmpClient);
    }

    @AfterAll
    static void afterAll() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
    }

    @Test
    void fetch_mapsFmpProfileToPriceQuote() throws Exception {
        String fixture = StreamUtils.copyToString(
                new ClassPathResource("fixtures/fmp/profile-inveb.json").getInputStream(),
                StandardCharsets.UTF_8);

        wireMock.stubFor(get(urlPathEqualTo("/profile"))
                .withQueryParam("symbol", equalTo("INVE-B.ST"))
                .withQueryParam("apikey", equalTo("test-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fixture)));

        Instant before = Instant.now();
        PriceQuote quote = provider.fetch(inveb);
        Instant after = Instant.now();

        assertEquals(245.5, quote.price());
        assertEquals("SEK", quote.currency());
        assertEquals(1_000_000_000d, quote.marketCap());
        assertEquals(-1.25, quote.changePercent24h());
        assertEquals(123_456d, quote.volume());
        assertFalse(quote.fetchedAt().isBefore(before));
        assertFalse(quote.fetchedAt().isAfter(after));
    }

    @Test
    void fetch_convertsGBpToGbpForLondonStocks() throws Exception {

        String fixture = StreamUtils.copyToString(
                new ClassPathResource("fixtures/fmp/profile-brwm.json").getInputStream(),
                StandardCharsets.UTF_8);

        wireMock.stubFor(get(urlPathEqualTo("/profile"))
                .withQueryParam("symbol", equalTo("BRWM.L"))
                .withQueryParam("apikey", equalTo("test-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fixture)));

        PriceQuote quote = provider.fetch(brwm);

        assertEquals(5.50, quote.price()); // 550.0 GBp normalized to 5.50 GBP
        assertEquals("GBP", quote.currency());
        assertEquals(1_400_000_000d, quote.marketCap());
        assertEquals(1.5, quote.changePercent24h());
        assertEquals(250_000d, quote.volume());
    }

    @Test
    void fetch_doesNotConvertGbpToGbpWhenAlreadyInPounds() throws Exception {

        String fixture = "[{\n" +
                "  \"symbol\": \"BRWM.L\",\n" +
                "  \"price\": 5.50,\n" +
                "  \"marketCap\": 1400000000,\n" +
                "  \"changePercentage\": 1.5,\n" +
                "  \"volume\": 250000,\n" +
                "  \"currency\": \"GBP\"\n" +
                "}]";

        wireMock.stubFor(get(urlPathEqualTo("/profile"))
                .withQueryParam("symbol", equalTo("BRWM.L"))
                .withQueryParam("apikey", equalTo("test-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fixture)));

        PriceQuote quote = provider.fetch(brwm);

        assertEquals(5.50, quote.price()); // 5.50 GBP stays 5.50 GBP
        assertEquals("GBP", quote.currency());
    }

    @Test
    void fetch_retrievesPriceInUsdForNyseStocks() throws Exception {

        String fixture = StreamUtils.copyToString(
                new ClassPathResource("fixtures/fmp/profile-o.json").getInputStream(),
                StandardCharsets.UTF_8);

        wireMock.stubFor(get(urlPathEqualTo("/profile"))
                .withQueryParam("symbol", equalTo("O"))
                .withQueryParam("apikey", equalTo("test-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fixture)));

        PriceQuote quote = provider.fetch(o);

        assertEquals(55.0, quote.price());
        assertEquals("USD", quote.currency());
        assertEquals(50_000_000_000d, quote.marketCap());
        assertEquals(-0.85, quote.changePercent24h());
        assertEquals(3_200_000d, quote.volume());
    }


    @Test
    void fetch_throwsExternalProviderExceptionOnCurrencyMismatch() {
        String fixture = "[{\n" +
                "  \"symbol\": \"INVE-B.ST\",\n" +
                "  \"price\": 245.5,\n" +
                "  \"marketCap\": 1000000000,\n" +
                "  \"changePercentage\": -1.25,\n" +
                "  \"volume\": 123456,\n" +
                "  \"currency\": \"USD\"\n" + // Configured is SEK
                "}]";

        wireMock.stubFor(get(urlPathEqualTo("/profile"))
                .withQueryParam("symbol", equalTo("INVE-B.ST"))
                .withQueryParam("apikey", equalTo("test-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fixture)));

        assertThrows(ExternalProviderException.class, () -> provider.fetch(inveb));
    }

    @Test
    void fetch_propagatesExternalProviderExceptionOnFailure() {
        wireMock.stubFor(get(urlPathEqualTo("/profile"))
                .withQueryParam("symbol", equalTo("INVE-B.ST"))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(ExternalProviderException.class, () -> provider.fetch(inveb));
    }

    @Test
    void fetch_propagatesExternalProviderExceptionOnEmptyResponse() {
        wireMock.stubFor(get(urlPathEqualTo("/profile"))
                .withQueryParam("symbol", equalTo("INVE-B.ST"))
                .withQueryParam("apikey", equalTo("test-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        assertThrows(ExternalProviderException.class, () -> provider.fetch(inveb));
    }

    @Test
    void fetch_rejectsNonFmpAsset() {
        AssetDefinition scrapeAsset = new AssetDefinition(
                "chdiv",
                "UBS ETF",
                AssetProvider.SCRAPE,
                "CHDIV.SW",
                "CHDIV",
                AssetType.ETF,
                "CHF",
                new MarketHours(ZoneId.of("Europe/Zurich"), LocalTime.of(9, 0), LocalTime.of(17, 30)),
                new SyncConfig(10, 0),
                "yahoo-six-chart");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> provider.fetch(scrapeAsset));

        assertEquals("FmpPriceProvider cannot fetch asset with provider: SCRAPE", error.getMessage());
    }

    @Test
    void providerId_returnsFmp() {
        assertEquals("fmp", provider.providerId());
    }
}
