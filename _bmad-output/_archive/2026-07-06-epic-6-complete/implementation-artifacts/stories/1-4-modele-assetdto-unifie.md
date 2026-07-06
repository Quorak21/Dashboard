---
baseline_commit: 35ff7e019c6faf63eee0585a9bda731d54513a27
---

# Story 1.4: Modèle AssetDto unifié

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

En tant que Dokk,
je veux un DTO JSON standard pour tous les actifs du registre,
afin que le front consomme un seul contrat.

## Acceptance Criteria

1. **Given** `ConfigurableAssetService.getData("inveb")` après sync réussie
   **When** le DTO est sérialisé en JSON
   **Then** la réponse contient `assetId`, `symbol` (db-symbol), `displayName`, `type`, `currency`, `currentPrice`, `marketCap`, `priceChangePercentage24h`, `totalVolume`, `lastRefresh`, `priceSource`, `marketStatus`, séries `history*` et `live*`

2. **And** les records Java sont immutables (NFR-12) ; blocs `dividends` et `fundamentals` **nullables** en attendant Epic 3

3. **And** enum `PriceSource` : `FMP`, `SCRAPE`, `CACHE`

_FRs : FR-2 · NFRs : NFR-12, NFR-13_

## Tasks / Subtasks

- [x] Créer les sous-records typés du contrat (AC: #2)
  - [x] `DividendsBlock`, `DividendHistoryEntry` — `BigDecimal` pour montants et `estimatedYield` (NFR-12)
  - [x] `FundamentalsBlock`, `HoldingEntry`, `SectorWeight` — `updatedAt` en `LocalDate`, `metrics` en `Map<String, Object>`
  - [x] Package `features/assets/model/` (singulier — aligné code existant, pas `models/` de l'architecture doc)
- [x] Finaliser `AssetDto` (AC: #1, #2)
  - [x] Remplacer `Object dividends` / `Object fundamentals` par `DividendsBlock` / `FundamentalsBlock` nullable
  - [x] Conserver factory `AssetDto.error(assetId, dbSymbol)` — blocs null, listes vides
  - [x] Vérifier que tous les champs prix/séries restent `Double` / `Long` (aligné entités JPA BACK-11)
- [x] Compléter `PriceSource` (AC: #3)
  - [x] Ajouter `SCRAPE` (prépare Epic 5 — pas d'appel scrape dans cette story)
  - [x] Mapper la source depuis le provider dans `ConfigurableAssetService.syncPrice` : `FMP` si `asset.provider() == FMP`, `SCRAPE` si `SCRAPE` (futur), `CACHE` en fallback
- [x] Tests sérialisation JSON (AC: #1)
  - [x] `AssetDtoTest` avec `ObjectMapper` Spring (ou `@JsonTest`) — fixture construite comme sortie post-sync inveb
  - [x] Assert JSON contient toutes les clés camelCase attendues ; `dividends` et `fundamentals` absents ou `null`
  - [x] Assert enums sérialisés en noms (`"FMP"`, `"OPEN"`, pas lowercase)
  - [x] Test round-trip désérialisation optionnel si utile pour contrat front
- [x] Mettre à jour `ConfigurableAssetServiceTest` si signatures `AssetDto` changent (compilation)
- [x] `mvn test` vert — **ne pas** modifier `DashboardController`, `InveBService`, loaders YAML

### Review Findings

- [x] [Review][Patch] NPE when PriceQuote or fetchedAt is Null [ConfigurableAssetService.java:79-80]
- [x] [Review][Patch] NPE when marketHours is Null [ConfigurableAssetService.java:170]
- [x] [Review][Patch] NPE when getLastRefresh is Null in buildLiveSeries [ConfigurableAssetService.java:181-183]
- [x] [Review][Patch] NPE when day is Null in refreshHistory [ConfigurableAssetService.java:207]
- [x] [Review][Patch] NPE when provider is Null [ConfigurableAssetService.java:232]
- [x] [Review][Defer] Blocking network call inside synchronized block [ConfigurableAssetService.java:70] — deferred, pre-existing
- [x] [Review][Defer] Unbounded cache maps (memory leak) [ConfigurableAssetService.java:40-42] — deferred, pre-existing
- [x] [Review][Defer] Unsynchronized refreshHistory database race [ConfigurableAssetService.java:199] — deferred, pre-existing
- [x] [Review][Defer] Suboptimal Live Series SQL query [ConfigurableAssetService.java:165] — deferred, pre-existing
- [x] [Review][Defer] Hardcoded JPA query limit names [AssetDailyRepository.java / AssetSnapshotRepository.java] — deferred, pre-existing
- [x] [Review][Defer] Missing transaction management on DB write [ConfigurableAssetService.java:152] — deferred, pre-existing
- [x] [Review][Defer] Inefficient sorting/reversing in Live Series [ConfigurableAssetService.java:165] — deferred, pre-existing

## Dev Notes

### Story scope — IN / OUT

| IN scope | OUT of scope (stories suivantes) |
|----------|----------------------------------|
| Records `AssetDto` + blocs typés nullables | Chargement YAML dividends/fundamentals (Epic 3) |
| Enum `PriceSource` complet (FMP, SCRAPE, CACHE) | `ScrapePriceProvider` / appels Yahoo (5.2) |
| Mapping provider → `PriceSource` dans `syncPrice` | Endpoint `GET /{assetId}` (1.5) |
| Tests JSON / contrat sérialisation | Types TypeScript front (4.1) |
| Ajustements compile tests existants | `computeEstimatedYield`, `fundamentals.stale` (3.3, 3.4) |
| | `MarketStatus.UNKNOWN` — optionnel ; OPEN/CLOSED suffisent pour inveb v1 |

**Ne pas toucher** `DashboardController`, `InveBService`, `AssetSyncJob`, `frontend/**`.

### État actuel du code (Story 1.3 — lire avant de modifier)

`AssetDto` existe déjà comme shell avec `Object` pour les blocs métier :

```java
// backend/.../features/assets/model/AssetDto.java — état actuel
public record AssetDto(
    String assetId, String symbol, String displayName, AssetType type, String currency,
    Double currentPrice, Double marketCap, Double priceChangePercentage24h, Double totalVolume,
    Long lastRefresh, PriceSource priceSource, MarketStatus marketStatus,
    List<Double> historyPrices, List<Long> historyDays,
    List<Double> livePrices, List<Long> liveDays,
    Object dividends, Object fundamentals) { ... }
```

`PriceSource` : `FMP`, `CACHE` seulement — **manque `SCRAPE`**.

`ConfigurableAssetService.syncPrice` hardcode `PriceSource.FMP` (ligne 95) — à remplacer par mapping provider.

`dividends` / `fundamentals` toujours `null` dans `syncPrice` et `assembleDto` — **conserver null** jusqu'à Epic 3.

### Contrat cible (ADR-09)

Records immutables — signatures cibles :

```java
public record AssetDto(
    String assetId,
    String symbol,              // db-symbol affiché (ex. "INVE-B")
    String displayName,
    AssetType type,
    String currency,
    Double currentPrice,
    Double marketCap,
    Double priceChangePercentage24h,
    Double totalVolume,
    Long lastRefresh,           // epoch ms
    PriceSource priceSource,    // FMP | SCRAPE | CACHE
    MarketStatus marketStatus,  // OPEN | CLOSED
    List<Double> historyPrices,
    List<Long> historyDays,
    List<Double> livePrices,
    List<Long> liveDays,
    DividendsBlock dividends,       // null — Epic 3
    FundamentalsBlock fundamentals  // null — Epic 3
) {}

public record DividendsBlock(
    BigDecimal forwardDividend,
    String forwardDividendCurrency,
    String frequency,
    BigDecimal estimatedYield,          // null jusqu'à Epic 3.3
    BigDecimal avgDividendGrowth10Y,    // nullable
    List<DividendHistoryEntry> history
) {}

public record DividendHistoryEntry(int year, BigDecimal amount, String currency) {}

public record FundamentalsBlock(
    LocalDate updatedAt,
    String source,
    boolean stale,                      // false par défaut — Epic 3.4
    Map<String, Object> metrics,
    List<HoldingEntry> topHoldings,
    List<SectorWeight> sectorWeights
) {}

public record HoldingEntry(String name, BigDecimal weightPercent) {}

public record SectorWeight(String sector, BigDecimal weightPercent) {}
```

**Décision implémentation :** créer les sous-records maintenant même si non peuplés — le contrat JSON est stable pour Story 4.1 (miroir TS). Pas besoin de factory builder ; `null` sur les blocs racine suffit.

### Mapping provider → PriceSource

```java
private PriceSource priceSourceFor(AssetDefinition asset, boolean fromCache) {
    if (fromCache) return PriceSource.CACHE;
    return switch (asset.provider()) {
        case FMP -> PriceSource.FMP;
        case SCRAPE -> PriceSource.SCRAPE;
    };
}
```

Utiliser dans `syncPrice` (succès) et `fallbackFromCache` (`CACHE`). Aucun actif `SCRAPE` en registre prod v1 — le case `SCRAPE` prépare Epic 5 sans test d'intégration scrape ici.

### JSON attendu (exemple post-sync inveb)

Clés **camelCase** (Jackson défaut Spring Boot — pas de `@JsonNaming` sur ces DTOs) :

```json
{
  "assetId": "inveb",
  "symbol": "INVE-B",
  "displayName": "Investor AB",
  "type": "STOCK",
  "currency": "SEK",
  "currentPrice": 245.5,
  "marketCap": 1000000000.0,
  "priceChangePercentage24h": -1.25,
  "totalVolume": 123456.0,
  "lastRefresh": 1748858400000,
  "priceSource": "FMP",
  "marketStatus": "OPEN",
  "historyPrices": [200.0],
  "historyDays": [1000],
  "livePrices": [],
  "liveDays": [],
  "dividends": null,
  "fundamentals": null
}
```

**Référence comparaison** : `InveBDto` expose moins de champs (pas `assetId`, `displayName`, `type`, `priceSource`, `marketStatus`). `AssetDto` est un **superset** — ne pas régresser les champs prix/séries partagés.

### Architecture compliance

| ADR | Application Story 1.4 |
|-----|----------------------|
| ADR-09 | DTO unifié + sous-blocs typés |
| ADR-01 | `ConfigurableAssetService` continue de construire `AssetDto` — pas de nouveau service |
| ADR-03 | `symbol` = `db-symbol` dans le DTO (déjà fait en 1.3) |
| ADR-06 | Structures blocs alignées YAML futur — **pas** de loader dans cette story |

Package cible (ajouts) :

```text
backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/
├── AssetDto.java              # UPDATE — typed blocks
├── PriceSource.java           # UPDATE — + SCRAPE
├── MarketStatus.java          # inchangé (OPEN, CLOSED)
├── AssetType.java             # inchangé
├── DividendsBlock.java        # NEW
├── DividendHistoryEntry.java  # NEW
├── FundamentalsBlock.java     # NEW
├── HoldingEntry.java          # NEW
└── SectorWeight.java          # NEW
```

**UPDATE** (pas NEW) :

```text
backend/.../features/assets/ConfigurableAssetService.java  # priceSource mapping
backend/.../features/assets/model/AssetDto.java
backend/.../features/assets/model/PriceSource.java
```

### Previous story intelligence (1.3)

- Shell `AssetDto` + enums créés ; tests service couvrent sync/cache/historique — **ne pas casser** les 9 tests existants.
- Review 1.3 : `fallbackFromCache` persiste cache avec `PriceSource.CACHE` ; `getData` recalcule `marketStatus` live via `MarketHoursGuard`.
- Story 1.3 note explicite : « Story 1.4 remplacera `Object` par `DividendsBlock` / `FundamentalsBlock` et ajoutera tests JSON ».
- `getData` ne appelle jamais provider — tests JSON peuvent appeler `syncPrice` puis sérialiser le résultat de `getData`.

### Previous story intelligence (1.2, 1.1)

- Fixture valeurs test : prix `245.5`, marketCap `1_000_000_000`, change `-1.25`, volume `123_456`, `fetchedAt` `2026-06-02T10:00:00Z`.
- Registre prod : seule entrée `inveb` (`config/assets-registry.yml`).
- `AssetProvider.SCRAPE` existe en enum — `providerId()` → `"scrape"` ; pas de provider bean avant Epic 5.

### File structure requirements

**NEW files :**

```text
backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/
  DividendsBlock.java
  DividendHistoryEntry.java
  FundamentalsBlock.java
  HoldingEntry.java
  SectorWeight.java

backend/src/test/java/com/dokkcorp/dashboard/features/assets/model/
  AssetDtoTest.java
```

**UPDATE files :**

```text
backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetDto.java
backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/PriceSource.java
backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java
```

**DO NOT MODIFY :**

```text
backend/src/main/java/com/dokkcorp/dashboard/controller/DashboardController.java
backend/src/main/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBService.java
backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java
frontend/**/*
```

### Testing requirements

**`AssetDtoTest`** (minimum 3 scénarios) :

| Test | Vérifie |
|------|---------|
| `serializesFullAssetDto_withAllPriceFields` | AC #1 — toutes clés JSON présentes, valeurs inveb fixture |
| `serializesNullDividendsAndFundamentals` | AC #2 — blocs null explicitement ou absents selon config Jackson |
| `priceSourceEnumValues_serializeAsNames` | AC #3 — `FMP`, `SCRAPE`, `CACHE` en JSON string |

Approche recommandée :

```java
@JsonTest
class AssetDtoTest {
    @Autowired ObjectMapper objectMapper;
    // ou new ObjectMapper() + JavaTimeModule pour LocalDate dans tests blocs futurs
}
```

Test unitaire optionnel `priceSourceFor_scrapeProvider_returnsScrape` — mock `AssetDefinition` avec `AssetProvider.SCRAPE` sans appel HTTP.

- `mvn test` depuis `backend/` — tous tests existants + nouveaux verts.
- Pas de `@SpringBootTest` obligatoire pour `AssetDtoTest`.
- Steve uniquement (`backend/**`).

### Anti-patterns à éviter

- **Ne pas** charger les YAML `config/dividends/` ou `config/fundamentals/` — Epic 3.
- **Ne pas** implémenter `computeEstimatedYield` ni peupler `estimatedYield` — Story 3.3.
- **Ne pas** ajouter route `GET /{assetId}` — Story 1.5.
- **Ne pas** créer `asset.dto.ts` front — Story 4.1.
- **Ne pas** migrer `InveBDto` vers `AssetDto` dans le controller — Story 4.5 / 6.6.
- **Ne pas** utiliser `double` pour `forwardDividend` / `estimatedYield` dans les records — `BigDecimal` (NFR-12).
- **Ne pas** renommer le package `model/` en `models/` — variance doc vs code ; le code existant prime.

### Latest technical notes

- **Spring Boot 4** + Jackson 3 — sérialisation records Java native ; pas d'annotation Lombok.
- Enums JSON : noms Java par défaut (`FMP`, pas `fmp`) — le front Story 4.1 devra aligner les unions TS.
- `AssetType` sérialisé `"STOCK"` | `"REIT"` | `"ETF"` | `"TRUST"`.
- `LocalDate` dans `FundamentalsBlock` nécessite `jackson-datatype-jsr310` (déjà transitif Spring Boot) — enregistrer `JavaTimeModule` si test hors contexte Spring.
- NFR-13 : documenter dans Completion Notes que le contrat JSON est figé pour miroir front Epic 4.

### Project Structure Notes

- Décision brownfield : package `features/assets/model/` (singulier) vs `models/` dans architecture §4 — **suivre le code 1.1–1.3**.
- `HypeDto` reste séparé (hors registre) — pas de fusion dans `AssetDto`.
- Dette BACK-11 : prix en `Double` dans DTO — ne pas migrer vers `BigDecimal` sur champs prix live dans cette story.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-1.4]
- [Source: _bmad-output/planning-artifacts/architecture.md#ADR-09]
- [Source: _bmad-output/planning-artifacts/architecture.md#ADR-06]
- [Source: _bmad-output/planning-artifacts/architecture.md#5-Interfaces-clés]
- [Source: backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetDto.java]
- [Source: backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java]
- [Source: backend/src/main/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBDto.java]
- [Source: _bmad-output/implementation-artifacts/stories/1-3-configurableassetservice-cache-sync-et-historique.md]
- [Source: docs/project-context.md#Contrat-API]

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash

### Debug Log References

- Resolved Jackson 3.0+ compatibility in tests: since JavaTimeModule is registered natively in Jackson 3, we simplified ObjectMapper configuration and verified ISO-8601 formatting for LocalDate.

### Completion Notes List

- Introduced unified `AssetDto` contract with typed and nullable `DividendsBlock` and `FundamentalsBlock` sub-records.
- Created sub-records `DividendHistoryEntry`, `DividendsBlock`, `HoldingEntry`, `SectorWeight`, and `FundamentalsBlock` using `BigDecimal` for precision and `LocalDate` for dates.
- Added `SCRAPE` to `PriceSource` enum and mapped it dynamically based on asset provider in `ConfigurableAssetService.syncPrice`.
- Created `AssetDtoTest` to verify camelCase JSON serialization, enum serialization, and sub-block serialization.
- Verified that all existing and new tests pass cleanly with `mvn test`.

### File List

- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/DividendHistoryEntry.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/DividendsBlock.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/HoldingEntry.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/SectorWeight.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/FundamentalsBlock.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/PriceSource.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetDto.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java
- backend/src/test/java/com/dokkcorp/dashboard/features/assets/model/AssetDtoTest.java

## Change Log

- 2026-06-18 — Ultimate context engine analysis completed — comprehensive developer guide created (bmad-create-story)
- 2026-06-18 — Story implemented completely, tests added and verified green.
