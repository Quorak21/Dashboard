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

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origins}")
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private HypeService hypeService;

    @Autowired
    private InveBService inveBService;

    @GetMapping("/hype")
    public HypeDto getLastHypeData() {

        return this.hypeService.getLastHypeData();

    }

    @GetMapping("/inveb")
    public InveBDto getLastInveBData() {
        return this.inveBService.getLastInveBData();
    }

}
