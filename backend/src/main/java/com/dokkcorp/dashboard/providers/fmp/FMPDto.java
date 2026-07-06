package com.dokkcorp.dashboard.providers.fmp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FMPDto(

        String symbol,

        @JsonAlias("price") double currentPrice,

        @JsonAlias("marketCap") double marketCap,

        @JsonAlias("changePercentage") double priceChangePercentage24h,

        @JsonAlias("volume") double totalVolume,

        String currency) {

}
