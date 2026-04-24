package com.dokkcorp.dashboard.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dokkcorp.dashboard.model.entity.AssetDaily;

@Repository
public interface AssetDailyRepository extends JpaRepository<AssetDaily, Long> {
    Optional<AssetDaily> findFirstBySymbolOrderByLastRefreshDesc(String symbol);

    List<AssetDaily> findTop288BySymbolOrderByLastRefreshAsc(String symbol);
}
