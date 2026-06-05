package com.dokkcorp.dashboard.features.crypto.hype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.dokkcorp.dashboard.features.crypto.hype.maths.HypeCalculator;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeBlockchainDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeChartsDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeHlpDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSummaryDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeSupplyDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeTimedDataDto;
import com.dokkcorp.dashboard.features.crypto.hype.models.HypeValuationDto;
import com.dokkcorp.dashboard.model.entity.AssetDaily;
import com.dokkcorp.dashboard.model.entity.AssetSnapshot;
import com.dokkcorp.dashboard.providers.blockchain.BlockChainDto;
import com.dokkcorp.dashboard.providers.hyperliquid.HyperliquidDto;
import com.dokkcorp.dashboard.repository.AssetDailyRepository;
import com.dokkcorp.dashboard.repository.AssetSnapshotRepository;

@Component
public class HypeMapper {

    private final AssetDailyRepository assetDailyRepository;
    private final AssetSnapshotRepository assetSnapshotRepository;
    private final HypeCalculator hypeCalculator;

    public HypeMapper(AssetDailyRepository assetDailyRepository, AssetSnapshotRepository assetSnapshotRepository,
            HypeCalculator hypeCalculator) {
        this.assetDailyRepository = assetDailyRepository;
        this.assetSnapshotRepository = assetSnapshotRepository;
        this.hypeCalculator = hypeCalculator;
    }

    public HypeDto toDto(AssetDaily entity, HyperliquidDto hyperliquidData, BlockChainDto blockchainData) {
        List<AssetSnapshot> history = new ArrayList<>(
                this.assetSnapshotRepository.findTop365BySymbolOrderByDayDesc("HYPE"));
        Collections.reverse(history);
        List<AssetDaily> daily = new ArrayList<>(
                this.assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc("HYPE"));
        Collections.reverse(daily);

        HypeSummaryDto summaryDto = getSummaryData(entity);
        HypeChartsDto chartsDto = getChartsData(history, daily);
        HypeTimedDataDto timedDataDto = getTimedData(hyperliquidData, daily, history);
        HypeSupplyDto supplyDto = getSupplyData(hyperliquidData);
        HypeBlockchainDto blockchainDto = getBlockchainData(hyperliquidData, blockchainData);
        HypeHlpDto hlpDto = getHlpData(hyperliquidData);
        HypeValuationDto valuationDto = getValuationData(hyperliquidData, entity, history);

        return new HypeDto(
                summaryDto,
                chartsDto,
                timedDataDto,
                supplyDto,
                blockchainDto,
                hlpDto,
                valuationDto);
    }

    private HypeSummaryDto getSummaryData(AssetDaily entity) {
        return new HypeSummaryDto(
                entity.getSymbol(),
                entity.getCurrentPrice(),
                entity.getMarketCap(),
                entity.getPriceChangePercentage24h(),
                entity.getTotalVolume(),
                entity.getLastRefresh().toEpochMilli());
    }

    private HypeChartsDto getChartsData(List<AssetSnapshot> history, List<AssetDaily> daily) {
        List<Double> historicalPrice = history.stream().map(AssetSnapshot::getPrice).toList();
        List<Long> historicalDays = history.stream()
                .map(s -> s.getDay().toEpochMilli())
                .toList();

        List<Double> livePrice = daily.stream().map(AssetDaily::getCurrentPrice).toList();
        List<Long> liveDay = daily.stream()
                .map(d -> d.getLastRefresh().toEpochMilli())
                .toList();

        List<AssetSnapshot> activitySnapshots = history.stream()
                .filter(s -> s.getVolume24h() != null && s.getOpenInterest() != null)
                .toList();
        List<Double> activityVolume = activitySnapshots.stream().map(AssetSnapshot::getVolume24h).toList();
        List<Double> activityOpenInterest = activitySnapshots.stream().map(AssetSnapshot::getOpenInterest).toList();
        List<Long> activityDays = activitySnapshots.stream()
                .map(s -> s.getDay().toEpochMilli())
                .toList();

        return new HypeChartsDto(
                historicalPrice,
                historicalDays,
                livePrice,
                liveDay,
                activityVolume,
                activityOpenInterest,
                activityDays);
    }

    private HypeTimedDataDto getTimedData(HyperliquidDto hyperliquidData, List<AssetDaily> daily,
            List<AssetSnapshot> history) {
        AssetDaily h24 = !daily.isEmpty() ? daily.get(0) : null;
        return this.hypeCalculator.computeTimedData(hyperliquidData, h24, history);
    }

    private HypeSupplyDto getSupplyData(HyperliquidDto hyperliquidData) {
        return this.hypeCalculator.computeSupplyData(hyperliquidData);
    }

    private HypeBlockchainDto getBlockchainData(HyperliquidDto hyperliquidData, BlockChainDto blockchainData) {
        return this.hypeCalculator.computeBlockchainData(blockchainData, hyperliquidData);
    }

    private HypeHlpDto getHlpData(HyperliquidDto hyperliquidData) {
        return this.hypeCalculator.computeHlpData(hyperliquidData);
    }

    private HypeValuationDto getValuationData(HyperliquidDto hyperliquidData, AssetDaily entity,
            List<AssetSnapshot> history) {
        return this.hypeCalculator.computeValuationData(hyperliquidData, entity, history);
    }
}
