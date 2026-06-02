package com.dokkcorp.dashboard.features.crypto.hype.maths;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dokkcorp.dashboard.features.crypto.hype.models.HypeBlockchainDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSupplyDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeValuationDto;
import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.providers.blockchain.BlockChainDto;
import com.dokkcorp.dashboard.providers.hyperliquid.HyperliquidDto;

class HypeCalculatorTest {

    private static final double EPS = 1e-8;

    private final HypeCalculator calculator = new HypeCalculator();

    @Test
    void computeSupplyData_keepsPrecisionOnRecurringRatio() {
        HyperliquidDto hyperliquidDto = new HyperliquidDto(
                "1",
                "0",
                "0",
                "0",
                "0",
                "0",
                "3",
                "1",
                "0");

        HypeSupplyDto result = calculator.computeSupplyData(hyperliquidDto);

        assertEquals(33.33333333d, result.circulating100(), EPS);
        assertEquals(0.0000001d, result.hypeBurned100(), EPS);
    }

    @Test
    void computeBlockchainData_returnsZeroWhenDenominatorIsZero() {
        HyperliquidDto hyperliquidDto = new HyperliquidDto(
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0");
        BlockChainDto blockChainDto = new BlockChainDto("123.45", "6.78");

        HypeBlockchainDto result = calculator.computeBlockchainData(blockChainDto, hyperliquidDto);

        assertEquals(0d, result.ratioBridged(), EPS);
        assertEquals(0d, result.stakedEvmCore(), EPS);
    }

    @Test
    void computeValuationData_usesBigDecimalForFeeAggregation() {
        HyperliquidDto hyperliquidDto = new HyperliquidDto(
                "1000",
                "0",
                "0",
                "0.1",
                "50",
                "12",
                "1000",
                "0",
                "100");

        AssetDaily entity = new AssetDaily();
        entity.setCurrentPrice(10d);
        entity.setMarketCap(100d);

        AssetSnapshot s1 = new AssetSnapshot();
        s1.setFees24h(0.1d);
        AssetSnapshot s2 = new AssetSnapshot();
        s2.setFees24h(0.2d);

        HypeValuationDto result = calculator.computeValuationData(hyperliquidDto, entity, List.of(s1, s2));

        assertEquals(54.75d, result.feesAnnual(), EPS);
        assertEquals(1.82648402d, result.ratioPriceFees(), EPS);
    }
}
