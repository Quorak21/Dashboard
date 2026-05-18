package com.dokkcorp.dashboard.providers.blockchain.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class HyperEvmRpc {

    @org.springframework.beans.factory.annotation.Value("${app.blockchain.rpc-url}")
    private String rpcUrl;

    //Création du client web3j avec le RPC Alchemy pour Hype core
    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(rpcUrl));
    }

}
