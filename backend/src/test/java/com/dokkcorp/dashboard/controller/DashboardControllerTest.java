package com.dokkcorp.dashboard.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.dokkcorp.dashboard.features.crypto.hype.HypeDto;
import com.dokkcorp.dashboard.features.crypto.hype.HypeService;
import com.dokkcorp.dashboard.features.assets.ConfigurableAssetService;
import com.dokkcorp.dashboard.features.assets.alerts.QuarterlyReportAlertService;
import com.dokkcorp.dashboard.features.assets.alerts.StaleAssetAlert;
import com.dokkcorp.dashboard.features.assets.model.AssetDto;
import com.dokkcorp.dashboard.features.assets.model.RegisteredAssetDto;
import com.dokkcorp.dashboard.features.assets.model.AssetType;
import com.dokkcorp.dashboard.features.assets.model.MarketStatus;
import com.dokkcorp.dashboard.features.assets.model.PriceSource;
import com.dokkcorp.dashboard.exception.GlobalExceptionHandler;
import com.dokkcorp.dashboard.exception.AssetNotFoundException;

import java.time.LocalDate;
import java.util.List;

class DashboardControllerTest {

    private MockMvc mockMvc;

    private HypeService hypeService;
    private ConfigurableAssetService configurableAssetService;
    private QuarterlyReportAlertService quarterlyReportAlertService;

    @BeforeEach
    void setUp() {
        hypeService = mock(HypeService.class);
        configurableAssetService = mock(ConfigurableAssetService.class);
        quarterlyReportAlertService = mock(QuarterlyReportAlertService.class);

        DashboardController controller = new DashboardController(
                hypeService, configurableAssetService, quarterlyReportAlertService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addPlaceholderValue("app.cors.allowed-origins", "http://localhost:4200")
                .build();
    }

    @Test
    void testGetLastHypeData() throws Exception {
        HypeDto mockHype = new HypeDto(null, null, null, null, null, null, null);
        when(hypeService.getLastHypeData()).thenReturn(mockHype);

        mockMvc.perform(get("/api/dashboard/hype"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAssetDataGenericRoute() throws Exception {
        AssetDto mockAsset = new AssetDto(
                "inveb",
                "INVE-B",
                "Investor AB",
                AssetType.STOCK,
                "SEK",
                250.0,
                2000000000.0,
                1.5,
                100000.0,
                123456789L,
                PriceSource.FMP,
                MarketStatus.OPEN,
                List.of(245.0, 250.0),
                List.of(12345L, 12346L),
                List.of(248.0, 250.0),
                List.of(1234567L, 1234568L),
                null,
                null
        );
        when(configurableAssetService.getData("inveb")).thenReturn(mockAsset);

        mockMvc.perform(get("/api/dashboard/asset/inveb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetId").value("inveb"))
                .andExpect(jsonPath("$.symbol").value("INVE-B"))
                .andExpect(jsonPath("$.displayName").value("Investor AB"))
                .andExpect(jsonPath("$.currentPrice").value(250.0));

        verify(configurableAssetService, times(1)).getData("inveb");
        verifyNoMoreInteractions(configurableAssetService);
    }

    @Test
    void testGetAssetDataDirectGenericRoute() throws Exception {
        AssetDto mockAsset = new AssetDto(
                "brwm",
                "BRWM",
                "World Mining",
                AssetType.STOCK,
                "GBP",
                500.0,
                1000000.0,
                -0.5,
                5000.0,
                123456789L,
                PriceSource.FMP,
                MarketStatus.OPEN,
                List.of(), List.of(), List.of(), List.of(),
                null, null
        );
        when(configurableAssetService.getData("brwm")).thenReturn(mockAsset);

        mockMvc.perform(get("/api/dashboard/asset/brwm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetId").value("brwm"))
                .andExpect(jsonPath("$.symbol").value("BRWM"));

        verify(configurableAssetService, times(1)).getData("brwm");
        verifyNoMoreInteractions(configurableAssetService);
    }

    @Test
    void testGetAssetDataUnknown() throws Exception {
        when(configurableAssetService.getData("unknown")).thenThrow(new AssetNotFoundException("unknown"));

        mockMvc.perform(get("/api/dashboard/asset/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Un truc n'a pas marché."));

        verify(configurableAssetService, times(1)).getData("unknown");
        verifyNoMoreInteractions(configurableAssetService);
    }

    @Test
    void testGetQuarterlyAlerts() throws Exception {
        StaleAssetAlert alert1 = new StaleAssetAlert(
                "inveb",
                "Investor AB",
                "INVE-B",              // label = ticker symbol (D2 decision)
                LocalDate.of(2026, 3, 15),
                102
        );
        when(quarterlyReportAlertService.getStaleAssets()).thenReturn(List.of(alert1));

        mockMvc.perform(get("/api/dashboard/alerts/quarterly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts[0].assetId").value("inveb"))
                .andExpect(jsonPath("$.alerts[0].displayName").value("Investor AB"))
                .andExpect(jsonPath("$.alerts[0].label").value("INVE-B"))
                .andExpect(jsonPath("$.alerts[0].updatedAt").value("2026-03-15"))
                .andExpect(jsonPath("$.alerts[0].daysStale").value(102));

        verify(quarterlyReportAlertService, times(1)).getStaleAssets();
        verifyNoMoreInteractions(quarterlyReportAlertService);
    }

    @Test
    void testGetRegisteredAssets() throws Exception {
        RegisteredAssetDto asset = new RegisteredAssetDto("inveb", "Investor AB", "STOCK", "SEK");
        when(configurableAssetService.getRegisteredAssets()).thenReturn(List.of(asset));

        mockMvc.perform(get("/api/dashboard/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("inveb"))
                .andExpect(jsonPath("$[0].displayName").value("Investor AB"))
                .andExpect(jsonPath("$[0].type").value("STOCK"))
                .andExpect(jsonPath("$[0].currency").value("SEK"));

        verify(configurableAssetService, times(1)).getRegisteredAssets();
        verifyNoMoreInteractions(configurableAssetService);
    }
}
