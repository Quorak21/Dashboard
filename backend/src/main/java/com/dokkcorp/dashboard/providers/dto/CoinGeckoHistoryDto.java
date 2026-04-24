package com.dokkcorp.dashboard.providers.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinGeckoHistoryDto(

                List<List<Double>> prices

) {

}
