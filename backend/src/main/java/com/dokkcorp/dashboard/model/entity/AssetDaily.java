package com.dokkcorp.dashboard.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "asset_daily")
public class AssetDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String symbol;
    private Double currentPrice;
    private Double marketCap;
    private Double priceChangePercentage24h;
    private Double totalVolume;
    private Long lastRefresh;
    private String burnedHype;
    private String circulatingSupply;
    private String feesDaily;
    private String dailyVolume;
    private String openInterest;
    private String totalValueLocked;

}
