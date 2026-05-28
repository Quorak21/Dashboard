package com.dokkcorp.dashboard.providers.hyperliquid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HyperliquidDto(

                String circulatingSupply,

                String providerTvl,

                String providerApr,

                String dailyVolume,

                String openInterest,

                String stakingApr,

                String maxSupply,

                String hypeBurned,

                String totalStakedHype) {

}
