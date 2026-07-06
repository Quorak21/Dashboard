package com.dokkcorp.dashboard.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dokkcorp.dashboard.features.crypto.hype.HypeDto;
import com.dokkcorp.dashboard.features.crypto.hype.HypeService;
import com.dokkcorp.dashboard.features.assets.ConfigurableAssetService;
import com.dokkcorp.dashboard.features.assets.alerts.QuarterlyReportAlertService;
import com.dokkcorp.dashboard.features.assets.alerts.QuarterlyAlertsResponse;
import com.dokkcorp.dashboard.features.assets.model.AssetDto;
import com.dokkcorp.dashboard.features.assets.model.RegisteredAssetDto;

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origins}")
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final HypeService hypeService;
    private final ConfigurableAssetService configurableAssetService;
    private final QuarterlyReportAlertService quarterlyReportAlertService;

    public DashboardController(
            HypeService hypeService,
            ConfigurableAssetService configurableAssetService,
            QuarterlyReportAlertService quarterlyReportAlertService) {
        this.hypeService = hypeService;
        this.configurableAssetService = configurableAssetService;
        this.quarterlyReportAlertService = quarterlyReportAlertService;
    }

    @GetMapping("/hype")
    public HypeDto getLastHypeData() {
        return this.hypeService.getLastHypeData();
    }

    @GetMapping("/asset/{assetId}")
    public AssetDto getAssetData(@PathVariable String assetId) {
        return this.configurableAssetService.getData(assetId);
    }

    @GetMapping("/alerts/quarterly")
    public QuarterlyAlertsResponse getQuarterlyAlerts() {
        return new QuarterlyAlertsResponse(List.copyOf(this.quarterlyReportAlertService.getStaleAssets()));
    }

    @GetMapping("/assets")
    public List<RegisteredAssetDto> getRegisteredAssets() {
        return this.configurableAssetService.getRegisteredAssets();
    }

}
