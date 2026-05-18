package com.dokkcorp.dashboard.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dokkcorp.dashboard.model.entity.AssetDaily;

import jakarta.transaction.Transactional;

@Repository
public interface AssetDailyRepository extends JpaRepository<AssetDaily, Long> {

    // La table qui contient les data toutes les 10 minutes

    // Récupérer le dernier élément inséré, la dernière mise à jour
    Optional<AssetDaily> findFirstBySymbolOrderByLastRefreshDesc(String symbol);

    // Récupérer les 144 derniers éléments pour le graphique pour la chart daily, les 24 dernière heures
    List<AssetDaily> findTop144BySymbolOrderByLastRefreshDesc(String symbol);

    @Transactional
    void deleteByLastRefreshBefore(Long timestamp);
}
