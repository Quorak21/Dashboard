package com.dokkcorp.dashboard.providers.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinGeckoDto(

        String symbol,

        @JsonAlias("current_price") double currentPrice,

        @JsonAlias("market_cap") double marketCap,

        @JsonAlias("price_change_percentage_24h") double priceChangePercentage24h,

        @JsonAlias("total_volume") double totalVolume) {

}
