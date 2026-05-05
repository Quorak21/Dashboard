package com.dokkcorp.dashboard.providers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HyperliquidDto(

                String circulatingSupply,

                String totalValueLocked,

                String apr,

                String dailyVolume,

                String openInterest,

                String stakingApr,

                String maxSupply,

                String hypeBurned,

                String totalStakedHype) {

}
