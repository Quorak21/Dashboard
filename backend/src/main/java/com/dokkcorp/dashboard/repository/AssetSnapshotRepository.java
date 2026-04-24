package com.dokkcorp.dashboard.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dokkcorp.dashboard.model.entity.AssetSnapshot;

@Repository
public interface AssetSnapshotRepository extends JpaRepository<AssetSnapshot, Long> {
    Optional<AssetSnapshot> findFirstByOrderByDayDesc();

    List<AssetSnapshot> findTop365BySymbolOrderByDayAsc(String symbol);
}
