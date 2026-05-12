package com.dokkcorp.dashboard.features.currency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dokkcorp.dashboard.providers.stocks.ForexClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class CurrencyService {

    @Autowired
    private ForexClient forexClient;

    public Map<String, Double> getRates() {
        Map<String, Double> rates = new HashMap<>();
        rates.put("USD_CHF", forexClient.getUsdChfRatio());
        rates.put("USD_EUR", forexClient.getUsdEurRatio());
        rates.put("SEK_CHF", forexClient.getSekChfRatio());
        rates.put("SEK_USD", forexClient.getSekUsdratio());
        return rates;
    }
}
