package com.dokkcorp.dashboard.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "asset_snapshot")
public class AssetSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String symbol;
    private Double price;
    private Long day;
    private Double volume24h;
    private Double fees24h;
    private Double hlpProvider;
    private Double openInterest;
    private String burnedHype;
    private String circulatingSupply;
}
