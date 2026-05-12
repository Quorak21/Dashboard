package com.dokkcorp.dashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dokkcorp.dashboard.features.crypto.hype.HypeDto;
import com.dokkcorp.dashboard.features.crypto.hype.HypeService;

import com.dokkcorp.dashboard.features.stocks.investorab.InveBDto;
import com.dokkcorp.dashboard.features.stocks.investorab.InveBService;
import com.dokkcorp.dashboard.features.currency.CurrencyService;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origins}")
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private HypeService hypeService;

    @Autowired
    private InveBService inveBService;

    @Autowired
    private CurrencyService currencyService;

    @GetMapping("/hype")
    public HypeDto getLastHypeData() {

        return this.hypeService.getLastHypeData();

    }

    @GetMapping("/inveb")
    public InveBDto getLastInveBData() {
        return this.inveBService.getLastInveBData();
    }

    @GetMapping("/rates")
    public Map<String, Double> getRates() {
        return this.currencyService.getRates();
    }

}
