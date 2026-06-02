package com.dokkcorp.dashboard.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.dokkcorp.dashboard.features.crypto.hype.HypeDto;
import com.dokkcorp.dashboard.features.crypto.hype.HypeService;
import com.dokkcorp.dashboard.features.stocks.investorab.InveBDto;
import com.dokkcorp.dashboard.features.stocks.investorab.InveBService;

class DashboardControllerTest {

    private final HypeService hypeService = mock(HypeService.class);
    private final InveBService inveBService = mock(InveBService.class);
    private final DashboardController controller = new DashboardController(hypeService, inveBService);

    @Test
    void getLastHypeData_returnsExactServicePayload() {
        HypeDto expected = HypeDto.error("HYPE");
        when(hypeService.getLastHypeData()).thenReturn(expected);

        HypeDto result = controller.getLastHypeData();

        assertSame(expected, result);
        assertEquals("HYPE", result.summary().symbol());
    }

    @Test
    void getLastInveBData_returnsExactServicePayload() {
        InveBDto expected = new InveBDto(
                "INVE-B",
                245.50,
                1_000_000_000d,
                -1.25d,
                123_456d,
                1_700_000_000d,
                java.util.List.of(1d, 2d),
                java.util.List.of(1L, 2L),
                java.util.List.of(3d, 4d),
                java.util.List.of(3L, 4L));
        when(inveBService.getLastInveBData()).thenReturn(expected);

        InveBDto result = controller.getLastInveBData();

        assertSame(expected, result);
        assertEquals("INVE-B", result.symbol());
        assertEquals(245.50, result.currentPrice());
    }
}
