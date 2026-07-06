---
story_id: 2.4
story_key: 2-4-snapshots-quotidiens-pour-actifs-registre
epic: 2
epic_name: Synchronisation prix et observabilité
status: done
baseline_commit: 35ff7e0
created: 2026-06-23
FRs:
  - FR-3
NFRs:
  - NFR-10
dependencies:
  - 1.3 (ConfigurableAssetService — persistance AssetDaily)
  - 2.2 (AssetSyncJob registry-driven, pattern try/catch par actif)
  - 2.3 (ProviderCallMetrics — pas de changement requis ici)
---

# Story 2.4 — Snapshots quotidiens pour actifs registre

## User Story

En tant que Dokk,
je veux des clôtures journalières en BD pour tous les actifs du registre,
afin d'alimenter les graphiques annuels (365 pts).

## Acceptance Criteria

### AC-1: Midnight UTC cron unchanged

**Given** `AssetSyncJob` est démarré
**When** minuit UTC est atteint
**Then** `sendDailySnapshotToDb()` s'exécute via `@Scheduled(cron = "0 0 0 * * ?", zone = "UTC")`

### AC-2: Registry loop creates price-only snapshots from latest AssetDaily

**Given** le registre contient un actif (ex. `inveb` avec `db-symbol: INVE-B`)
**And** des points `AssetDaily` existent pour ce `db-symbol`
**When** `sendDailySnapshotToDb()` s'exécute
**Then** une boucle sur `assetRegistry.all()` crée un `AssetSnapshot` par actif registre avec :
- `symbol` = `asset.dbSymbol()`
- `price` = `AssetDaily.currentPrice` du dernier point (`findFirstBySymbolOrderByLastRefreshDesc`)
- `day` = `AssetDaily.lastRefresh` du même point
**And** les champs HYPE-spécifiques (`volume24h`, `fees24h`, `hlpProvider`, `openInterest`, `burnedHype`, `circulatingSupply`) restent `null` pour les actifs registre

### AC-3: Skip snapshot when price is null

**Given** un actif registre avec un dernier `AssetDaily` dont `currentPrice` est `null`
**When** le snapshot est tenté
**Then** aucun `AssetSnapshot` n'est sauvegardé pour cet actif
**And** log WARN explicite (pattern existant INVE-B : `Snapshot {dbSymbol} reporté : données dégradées (currentPrice null)`)

### AC-4: Skip snapshot when no daily data

**Given** un actif registre sans ligne `AssetDaily` pour son `db-symbol`
**When** le snapshot est tenté
**Then** l'actif est ignoré avec log ERROR (pattern `sendHypeSnapshot` : pas de `orElseThrow` qui bloque les autres actifs — catch par actif)

### AC-5: Per-symbol failure isolation

**Given** plusieurs actifs dans le registre (présent ou futur : inveb, brwm, o, iii, infr, chdiv, qqqe)
**When** la sauvegarde échoue pour un symbole (exception repository, données invalides)
**Then** les autres actifs du registre sont quand même traités
**And** l'échec est loggé ERROR avec le `db-symbol` ou `assetId`

### AC-6: HYPE dedicated snapshot unchanged

**Given** des données HYPE valides en `AssetDaily` et `HypeService`
**When** `sendDailySnapshotToDb()` s'exécute
**Then** `sendHypeSnapshot()` conserve sa logique dédiée existante (champs enrichis volume/fees/OI/HLP/burn/supply)
**And** HYPE n'est **pas** dans le registre YAML — il n'apparaît pas dans la boucle registre

### AC-7: InveBService removed from snapshot path

**Given** `sendInveBSnapshot()` utilisait `InveBService.getLastInveBData()` pour le prix
**When** la migration est complète
**Then** `sendInveBSnapshot()` est supprimée
**And** `InveBService` n'est plus injecté dans `AssetSyncJob` (plus aucune référence)
**And** le prix INVE-B provient uniquement de `AssetDaily` (cohérent avec `ConfigurableAssetService.syncPrice`)

### AC-8: cleanDB retention unchanged

**Given** `cleanDB()` planifié le dimanche minuit
**When** la story est livrée
**Then** rétention inchangée : 7 jours `AssetDaily`, 365 jours `AssetSnapshot`
**And** tests `cleanDb_usesExpectedRetentionWindows` passent sans modification de comportement

### AC-9: Unit tests updated

**Given** `AssetSyncJobTest`
**When** les tests snapshot sont exécutés
**Then** ils valident : HYPE + registre (INVE-B), timestamps `day`, isolation échec par actif, skip prix null, skip liste registre vide
**And** `mvnw test` — tous les tests passent

---

## Tasks / Subtasks

- [x] Refactor `sendDailySnapshotToDb()` — boucle registre (AC: #2, #5, #6)
  - [x] Ajouter méthode privée `sendRegistrySnapshots()` itérant `assetRegistry.all()`
  - [x] Pour chaque `AssetDefinition` : try/catch, lookup `findFirstBySymbolOrderByLastRefreshDesc(asset.dbSymbol())`
  - [x] Créer `AssetSnapshot` price-only (symbol, price, day)
  - [x] Conserver `sendHypeSnapshot()` inchangée en tête de `sendDailySnapshotToDb()`
- [x] Supprimer legacy InveB snapshot path (AC: #7)
  - [x] Supprimer `sendInveBSnapshot()` et l'injection `InveBService` du constructeur
  - [x] Retirer imports `InveBService`, `InveBDto`
- [x] Mettre à jour `AssetSyncJobTest` (AC: #9)
  - [x] Mocker `assetRegistry.all()` pour les tests snapshot INVE-B
  - [x] Retirer dépendance `inveBService` du constructeur de test
  - [x] Ajouter tests : isolation échec, prix null skip, registre vide
- [x] Vérifier `mvnw test` complet

### Review Findings

- [x] [Review][Patch] Risque de duplication de snapshots ou d'exception de contrainte d'unicité lors des jours fermés (week-ends et jours fériés) [AssetSyncJob.java:97]
- [x] [Review][Patch] Risque de snapshot avec un champ day nul [AssetSyncJob.java:115]
- [x] [Review][Patch] Commentaire obsolète faisant référence à INVE-B uniquement [AssetSyncJob.java:91]
- [x] [Review][Defer] Risque de performance (requêtes N+1) sur la boucle de snapshots [AssetSyncJob.java:97] — deferred, pre-existing
- [x] [Review][Defer] Absence de planification automatique pour d'autres providers (ex. SCRAPE) [AssetSyncJob.java:69] — deferred, pre-existing
- [x] [Review][Defer] La planification de synchronisation ignore les configurations d'intervalles et d'offsets du registre YAML [AssetSyncJob.java:69] — deferred, pre-existing
- [x] [Review][Defer] Risque de NullPointerException dans MarketHoursGuard.isOpen lors de l'instanciation de tests [MarketHoursGuard.java:1] — deferred, pre-existing
- [x] [Review][Defer] Appels réseau bloquants et séquentiels dans syncFmpAssets [AssetSyncJob.java:69] — deferred, pre-existing
- [x] [Review][Defer] Bloc synchronisé englobant un appel réseau bloquant dans ConfigurableAssetService [ConfigurableAssetService.java:1] — deferred, pre-existing
- [x] [Review][Defer] Absence de limitation de débit sur les appels externes FMP [AssetSyncJob.java:69] — deferred, pre-existing


---

## Dev Notes

### Current State (READ BEFORE CODING)

`AssetSyncJob.sendDailySnapshotToDb()` appelle aujourd'hui deux méthodes hardcodées :

```95:100:backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java
    public void sendDailySnapshotToDb() {
        // Chaque symbole est indépendant : un HYPE dégradé ne doit pas
        // empêcher la sauvegarde du snapshot INVE-B (et inversement).
        sendHypeSnapshot();
        sendInveBSnapshot();
    }
```

`sendInveBSnapshot()` utilise encore `InveBService` pour le prix alors que `ConfigurableAssetService` persiste déjà les points `AssetDaily` via `syncFmpAssets()` (Story 2.2). La Story 2.4 aligne le snapshot sur la source de vérité BD.

`AssetRegistry.all()` retourne tous les actifs YAML — actuellement seul `inveb`. HYPE n'est pas dans le registre.

### Target Design

```java
@Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
public void sendDailySnapshotToDb() {
    sendHypeSnapshot();
    sendRegistrySnapshots();
}

private void sendRegistrySnapshots() {
    List<AssetDefinition> assets = assetRegistry.all();
    if (assets == null) {
        return;
    }
    for (AssetDefinition asset : assets) {
        if (asset == null) {
            continue;
        }
        try {
            saveRegistrySnapshot(asset);
        } catch (Exception e) {
            logger.error("Sauvegarde du snapshot {} ({}) n'a pas fonctionné",
                    asset.dbSymbol(), asset.id(), e);
        }
    }
}

private void saveRegistrySnapshot(AssetDefinition asset) {
    Optional<AssetDaily> latest = assetDailyRepository
            .findFirstBySymbolOrderByLastRefreshDesc(asset.dbSymbol());
    if (latest.isEmpty()) {
        logger.warn("Snapshot {} reporté : aucun point AssetDaily", asset.dbSymbol());
        return;
    }
    AssetDaily ad = latest.get();
    Double price = ad.getCurrentPrice();
    if (price == null) {
        logger.warn("Snapshot {} reporté : données dégradées (currentPrice null)", asset.dbSymbol());
        return;
    }
    AssetSnapshot snapshot = new AssetSnapshot();
    snapshot.setSymbol(asset.dbSymbol());
    snapshot.setPrice(price);
    snapshot.setDay(ad.getLastRefresh());
    assetSnapshotRepository.save(snapshot);
}
```

**Ne pas** appeler `configurableAssetService` ni `inveBService` dans le chemin snapshot — lecture BD uniquement.

### Behavior Change (intentional)

| Aspect | Avant (`sendInveBSnapshot`) | Après (registre) |
|--------|----------------------------|------------------|
| Prix INVE-B | `inveBService.getLastInveBData().currentPrice()` | `AssetDaily.currentPrice` |
| Dépendance | `InveBService` injecté dans job | Supprimée du job |
| Extensibilité | Hardcode INVE-B | Tout actif registre (FMP + scrape futurs) |

Le prix daily est alimenté par `ConfigurableAssetService.persistDailyPoint()` pendant les heures marché — suffisant pour le graphique annuel 365 pts consommé par `refreshHistory()`.

### What Must NOT Change

- `sendHypeSnapshot()` — logique enrichie crypto inchangée (NFR-10 : cron HYPE 10 min séparé aussi inchangé)
- `cleanDB()` — cron `0 0 0 * * SUN`, 7j daily / 365j snapshot
- `syncFmpAssets()` / `autoSync()` — hors scope
- Entités JPA `AssetDaily`, `AssetSnapshot` — pas de migration schéma
- `ConfigurableAssetService.refreshHistory()` — lit déjà `findTop365BySymbolOrderByDayDesc(asset.dbSymbol())`

### Project Structure Notes

**Modified files only:**
- `backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java`
- `backend/src/test/java/com/dokkcorp/dashboard/jobs/AssetSyncJobTest.java`

**No new files expected.**

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-2.4](_bmad-output/planning-artifacts/epics.md)
- [Source: _bmad-output/planning-artifacts/architecture.md#ADR-10](_bmad-output/planning-artifacts/architecture.md) — `sendDailySnapshots()` boucle registre + hype
- [Source: Story 2.2](2-2-assetsyncjob-tier-fmp-registry-driven.md) — conserver `InveBService` jusqu'à 2.4
- [Source: Story 1.3](1-3-configurableassetservice-cache-sync-et-historique.md) — persistance `AssetDaily` / lecture `AssetSnapshot`

---

## Dev Agent Record

### Agent Model Used

Composer (dev-story 2.4)

### Implementation Plan

1. Replace `sendInveBSnapshot()` with `sendRegistrySnapshots()` + `saveRegistrySnapshot()` looping `assetRegistry.all()`.
2. Remove `InveBService` from `AssetSyncJob` constructor — price sourced from `AssetDaily.currentPrice`.
3. Update tests: registry mocks, 3 new edge-case tests, remove `InveBService` mock.

### Debug Log References

- Isolation test calls `sendDailySnapshotToDb()` without HYPE mocks — registry-only path via direct registry loop (no HYPE in test).

### Completion Notes List

- All 9 acceptance criteria satisfied.
- `mvnw test` — 82 tests, 0 failures (3 new tests added).
- `InveBService` removed from `AssetSyncJob`; class remains in codebase for Epic 4.5.
- Registry snapshots are price-only (`volume24h`, etc. remain null).

### File List

- `backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java` (modified)
- `backend/src/test/java/com/dokkcorp/dashboard/jobs/AssetSyncJobTest.java` (modified)

## Change Log

- 2026-06-23: Story 2.4 — registry-driven daily snapshots, removed InveBService from AssetSyncJob, 3 new unit tests.

---

## Developer Context

### Architecture Compliance

- **ADR-10** : `sendDailySnapshots()` = boucle registre + hype dédié — cible explicite de cette story
- **FR-3** : Persistance historique unifiée — snapshots quotidiens pour alimenter graphiques 365 pts via `ConfigurableAssetService.refreshHistory()`
- **NFR-10** : Tier HYPE inchangé — ne pas toucher `autoSync()` ni `sendHypeSnapshot()`
- **db-symbol** : clé JPA `AssetDaily.symbol` / `AssetSnapshot.symbol` (architecture §3) — utiliser `asset.dbSymbol()`, jamais `asset.id()` ni `asset.symbol()` (FMP ticker)

### Technical Requirements

#### AssetRegistry usage

```java
List<AssetDefinition> assets = assetRegistry.all();
```

- Interface : `com.dokkcorp.dashboard.features.assets.AssetRegistry`
- Actuellement 1 entrée : `inveb` → `db-symbol: INVE-B`
- Epic 6 ajoutera brwm, o, iii, infr — la boucle doit fonctionner sans modification
- Epic 5 ajoutera scrape (chdiv, qqqe) — snapshots price-only suffisants (champs crypto null)

#### Repository methods (existing — do not add new queries)

```java
assetDailyRepository.findFirstBySymbolOrderByLastRefreshDesc(String symbol)
assetSnapshotRepository.save(AssetSnapshot)
```

#### Null-safety patterns (from Epic 1 retro AI-1)

- `currentPrice` nullable → skip + WARN (pas de NPE auto-unboxing)
- `lastRefresh` nullable → acceptable sur snapshot mais idéalement présent ; si null, `day` reste null (cohérent entité)
- Liste registre null → early return (pattern `syncFmpAssets`)
- Entrée registre null dans liste → skip (pattern Story 2.2)

#### Logging conventions

| Event | Level | Message pattern |
|-------|-------|-----------------|
| Snapshot saved | _(none — silent success)_ | — |
| Prix null | WARN | `Snapshot {dbSymbol} reporté : données dégradées (currentPrice null)` |
| Pas de daily | WARN | `Snapshot {dbSymbol} reporté : aucun point AssetDaily` |
| Exception | ERROR | `Sauvegarde du snapshot {dbSymbol} ({assetId}) n'a pas fonctionné` |

#### Constructor change

Remove `InveBService` parameter from `AssetSyncJob` constructor. Update Spring wiring automatically via constructor injection. Update `AssetSyncJobTest` mock setup accordingly.

### File Structure Requirements

| File | Action |
|------|--------|
| `backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java` | MODIFY — registry snapshots, remove InveBService |
| `backend/src/test/java/com/dokkcorp/dashboard/jobs/AssetSyncJobTest.java` | MODIFY — registry mocks, new edge-case tests |

### Testing Requirements

#### Update existing tests

1. **`sendDailySnapshotToDb_savesHypeAndInvebSnapshots`** — mock `assetRegistry.all()` returning `inveb` definition with `dbSymbol="INVE-B"` ; remove `inveBService` stub ; stub `assetDailyRepository` for INVE-B with price
2. **`sendDailySnapshotToDb_usesDailyTimestampForHypeSnapshot`** — same registry mock ; verify INVE-B snapshot uses `AssetDaily.lastRefresh` for `day` and `AssetDaily.currentPrice` for `price`

#### New tests to add

3. **`sendRegistrySnapshots_skipsNullPrice`** — daily exists but `currentPrice=null` → 0 save for registry asset (HYPE still saves if mocked)
4. **`sendRegistrySnapshots_isolatesFailuresPerAsset`** — two registry assets, first throws on save → second still saved
5. **`sendRegistrySnapshots_handlesEmptyRegistry`** — `all()` returns empty list → only HYPE snapshot (if mocked)

#### Test pattern

Continue Mockito direct (no `@SpringBootTest`) — consistent with Stories 2.2 and 2.3.

`AssetDefinition` is a Java record — use full constructor or minimal stub:

```java
new AssetDefinition("inveb", "Investor AB", AssetProvider.FMP,
        "INVE-B.ST", "INVE-B", AssetType.STOCK, "SEK", null, null, null);
```

Import `AssetType` from `com.dokkcorp.dashboard.features.assets.model.AssetType`.

#### Regression scope

Run full `mvnw test` — baseline ~79 tests after Story 2.3.

### Previous Story Intelligence

#### From Story 2.2

- **Explicit deferral** : « L'injection `InveBService` dans `AssetSyncJob` doit être conservée uniquement pour `sendInveBSnapshot()`, jusqu'à ce que la Story 2.4 vienne migrer » — **c'est le moment de supprimer cette injection**.
- **Try-catch par actif** : même pattern que `sendDailySnapshotToDb` commentaire existant — réutiliser, ne pas réinventer.
- **Null safety** : null dans liste registre → `continue` (déjà codé dans `syncFmpAssets`).

#### From Story 2.3

- Pas d'interaction avec `ProviderCallMetrics` pour les snapshots.
- Tests Mockito purs — pas de Spring context pour `AssetSyncJobTest`.

#### From Story 1.3

- `ConfigurableAssetService.persistDailyPoint()` écrit `AssetDaily` avec `asset.dbSymbol()` — le snapshot doit lire la même clé.
- `refreshHistory()` consomme `AssetSnapshot` triés par `day` desc, max 365 — la story alimente directement ce pipeline.

### Git Intelligence Summary

Commits récents (`35ff7e0` et suivants) concernent le frontend HYPE (charts, UI) — pas de pattern backend à répliquer. Le backend stable depuis Stories 2.2–2.3 : `AssetSyncJob`, `ConfigurableAssetService`, `ProviderCallMetrics`.

### Library / Framework Requirements

- Spring `@Scheduled` — cron existant, ne pas modifier
- `jakarta.transaction.Transactional` sur `cleanDB()` — inchangé
- JPA repositories existants — pas de nouvelle dépendance
- Lombok `@Data` sur entités — setters existants

### Latest Tech Information

- Spring Boot 4.0.6 / Java 21 — constructor injection standard
- Pas de recherche API externe requise — story 100% lecture/écriture BD locale
- `InveBService` reste dans le codebase (déprécié, Epic 4.5 facade) — seulement retiré de `AssetSyncJob`

### Project Context Reference

- **Steve** (@steve) — story 100% backend, périmètre `backend/**/*`
- DTOs = records ; entités JPA = Lombok `@Data` avec `Double` nullable (BACK-11 safe mode)
- Pas de modification `code_review.md` / `journal.md` (rôle @odin)
- VPS RAM : pas de cache supplémentaire — une requête `findFirst` par actif à minuit est négligeable

---

## Story Completion Status

**Status**: review
**Notes**: Implementation complete — registry snapshots from AssetDaily, HYPE path preserved, all tests pass.
