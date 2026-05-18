package com.dokkcorp.dashboard.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dokkcorp.dashboard.model.entity.AssetSnapshot;

@Repository
public interface AssetSnapshotRepository extends JpaRepository<AssetSnapshot, Long> {

    // La table qui contient 1an de data avec snapshot tous les jours

    // Récupérer le dernier snapshot créé
    Optional<AssetSnapshot> findFirstByOrderByDayDesc();

    // Récupérer le dernier snapshot pour un symbole
    Optional<AssetSnapshot> findFirstBySymbolOrderByDayDesc(String symbol);

    // Récupérer les 365 derniers snapshots pour un symbole
    List<AssetSnapshot> findTop365BySymbolOrderByDayDesc(String symbol);

    void deleteByDayBefore(Long timestamp);
}
