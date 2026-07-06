package com.dokkcorp.dashboard.features.assets.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AssetDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesFullAssetDto_withAllPriceFields() throws Exception {
        AssetDto dto = new AssetDto(
                "inveb",
                "INVE-B",
                "Investor AB",
                AssetType.STOCK,
                "SEK",
                245.5,
                1000000000.0,
                -1.25,
                123456.0,
                1748858400000L,
                PriceSource.FMP,
                MarketStatus.OPEN,
                List.of(200.0),
                List.of(1000L),
                List.of(),
                List.of(),
                null,
                null
        );

        String json = objectMapper.writeValueAsString(dto);

        // Assert camelCase keys
        assertTrue(json.contains("\"assetId\":\"inveb\""));
        assertTrue(json.contains("\"symbol\":\"INVE-B\""));
        assertTrue(json.contains("\"displayName\":\"Investor AB\""));
        assertTrue(json.contains("\"type\":\"STOCK\""));
        assertTrue(json.contains("\"currency\":\"SEK\""));
        assertTrue(json.contains("\"currentPrice\":245.5"));
        assertTrue(json.contains("\"marketCap\":1.0E9") || json.contains("\"marketCap\":1000000000.0") || json.contains("\"marketCap\":1000000000"));
        assertTrue(json.contains("\"priceChangePercentage24h\":-1.25"));
        assertTrue(json.contains("\"totalVolume\":123456.0"));
        assertTrue(json.contains("\"lastRefresh\":1748858400000"));
        assertTrue(json.contains("\"priceSource\":\"FMP\""));
        assertTrue(json.contains("\"marketStatus\":\"OPEN\""));
        assertTrue(json.contains("\"historyPrices\":[200.0]"));
        assertTrue(json.contains("\"historyDays\":[1000]"));
        assertTrue(json.contains("\"livePrices\":[]"));
        assertTrue(json.contains("\"liveDays\":[]"));
        assertTrue(json.contains("\"dividends\":null"));
        assertTrue(json.contains("\"fundamentals\":null"));
    }

    @Test
    void serializesNullDividendsAndFundamentals() throws Exception {
        AssetDto dto = AssetDto.error("inveb", "INVE-B", "Investor AB", AssetType.STOCK);
        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"displayName\":\"Investor AB\""));
        assertTrue(json.contains("\"dividends\":null"));
        assertTrue(json.contains("\"fundamentals\":null"));
    }

    @Test
    void priceSourceEnumValues_serializeAsNames() throws Exception {
        assertEquals("\"FMP\"", objectMapper.writeValueAsString(PriceSource.FMP));
        assertEquals("\"SCRAPE\"", objectMapper.writeValueAsString(PriceSource.SCRAPE));
        assertEquals("\"CACHE\"", objectMapper.writeValueAsString(PriceSource.CACHE));
    }

    @Test
    void serializesSubBlocksCorrectly_whenPopulated() throws Exception {
        DividendHistoryEntry divHistory = new DividendHistoryEntry(2025, new BigDecimal("4.50"), "SEK");
        DividendsBlock dividends = new DividendsBlock(
                new BigDecimal("5.00"),
                "SEK",
                "ANNUAL",
                new BigDecimal("0.02"),
                new BigDecimal("0.05"),
                List.of(divHistory)
        );

        HoldingEntry holding = new HoldingEntry("ABB", new BigDecimal("8.5"));
        SectorWeight sector = new SectorWeight("Industrials", new BigDecimal("45.2"));
        FundamentalsBlock fundamentals = new FundamentalsBlock(
                LocalDate.of(2026, 6, 18),
                "FMP",
                false,
                Map.of("peRatio", 18.5),
                List.of(holding),
                List.of(sector),
                List.of()
        );

        AssetDto dto = new AssetDto(
                "inveb",
                "INVE-B",
                "Investor AB",
                AssetType.STOCK,
                "SEK",
                245.5,
                1000000000.0,
                -1.25,
                123456.0,
                1748858400000L,
                PriceSource.FMP,
                MarketStatus.OPEN,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                dividends,
                fundamentals
        );

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"forwardDividend\":5.00") || json.contains("\"forwardDividend\":5"));
        assertTrue(json.contains("\"forwardDividendCurrency\":\"SEK\""));
        assertTrue(json.contains("\"frequency\":\"ANNUAL\""));
        assertTrue(json.contains("\"estimatedYield\":0.02"));
        assertTrue(json.contains("\"avgDividendGrowth10Y\":0.05"));
        assertTrue(json.contains("\"year\":2025"));
        assertTrue(json.contains("\"amount\":4.50") || json.contains("\"amount\":4.5"));
        
        assertTrue(json.contains("\"updatedAt\":\"2026-06-18\""));
        assertTrue(json.contains("\"source\":\"FMP\""));
        assertTrue(json.contains("\"stale\":false"));
        assertTrue(json.contains("\"peRatio\":18.5"));
        assertTrue(json.contains("\"name\":\"ABB\""));
        assertTrue(json.contains("\"weightPercent\":8.5"));
        assertTrue(json.contains("\"sector\":\"Industrials\""));
        assertTrue(json.contains("\"weightPercent\":45.2"));
    }
}
