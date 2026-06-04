package com.dokkcorp.dashboard.providers.blockchain.utils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class HyperEvmRpc {

    @Value("${app.blockchain.rpc-url}")
    private String rpcUrl;

    @Value("${app.blockchain.connect-timeout-ms:2000}")
    private long connectTimeoutMs;

    @Value("${app.blockchain.read-timeout-ms:5000}")
    private long readTimeoutMs;

    //Création du client web3j avec le RPC Alchemy pour Hype core
    @Bean
    public Web3j web3j() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .callTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .build();
        return Web3j.build(new HttpService(rpcUrl, httpClient, false));
    }

}
