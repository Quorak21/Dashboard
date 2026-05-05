package com.dokkcorp.dashboard.providers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class HyperEvmRpc {

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService("https://hyperliquid-mainnet.g.alchemy.com/v2/2GJ2xHyFi8xCaBv0sUHPj"));
    }

}
