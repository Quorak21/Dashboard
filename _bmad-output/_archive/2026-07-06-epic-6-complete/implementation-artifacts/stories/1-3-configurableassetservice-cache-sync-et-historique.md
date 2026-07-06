---
baseline_commit: 35ff7e019c6faf63eee0585a9bda731d54513a27
---

# Story 1.3: ConfigurableAssetService — cache, sync et historique

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

En tant que Dokk,
je veux un service unique qui gère cache, sync prix et séries BD,
afin de remplacer la logique dupliquée d'InveBService.

## Acceptance Criteria

1. **Given** un `assetId` présent dans le registre et des données historiques en BD sous `db-symbol`
   **When** `syncPrice(assetId)` est appelé pendant heures marché
   **Then** le service appelle `PriceProviderRegistry` → `FmpPriceProvider`, met à jour le cache et écrit un point `AssetDaily` avec le `db-symbol`

2. **When** `getData(assetId)` est appelé et le cache est peuplé
   **Then** il retourne le cache **sans** appel provider supplémentaire (NFR-3)

3. **And** `historyPrices` / `historyDays` proviennent de `AssetSnapshot` (365 pts) et `livePrices` / `liveDays` de la session courante `AssetDaily` (≤ 144 pts) — même logique qu'`InveBService`

4. **And** en cas d'échec provider, retourne le cache stale avec `priceSource: CACHE` si disponible

5. **And** hors heures marché, `syncPrice` ne persiste **pas** de point `AssetDaily` (NFR-2)

6. **And** tests unitaires couvrent sync OK, fallback cache, skip écriture hors heures marché

_FRs : FR-1, FR-3 · NFRs : NFR-2, NFR-3, NFR-6_

## Tasks / Subtasks

- [x] Créer `MarketHoursGuard` (AC: #1, #5)
  - [x] `boolean isOpen(AssetDefinition asset)` depuis `asset.marketHours()` (zone, open, close)
  - [x] Week-end exclus ; bornes open/close inclusives (aligné `InveBService` : `!time.isBefore(open) && !time.isAfter(close)`)
  - [x] `MarketStatus` dérivé : `OPEN` si ouvert, `CLOSED` sinon
- [x] Introduire DTO cache minimal (AC: #2, #4 — shell pour Story 1.4)
  - [x] `AssetDto` record dans `features/assets/model/` — champs prix + séries + `priceSource` + `marketStatus`
  - [x] `PriceSource` enum : `FMP`, `CACHE` (pas `SCRAPE` avant Epic 5)
  - [x] `MarketStatus` enum : `OPEN`, `CLOSED`
  - [x] `dividends` / `fundamentals` = `null` (Epic 3)
  - [x] Factory `AssetDto.error(assetId, dbSymbol)` pour état dégradé sans cache
- [x] Implémenter `ConfigurableAssetService` (AC: #1–#5)
  - [x] `Map<String, AtomicReference<AssetDto>>` — un cache par `assetId` (NFR-6)
  - [x] `syncPrice(String assetId)` : résolution registre → provider → `PriceQuote` → cache + `AssetDaily` si marché ouvert
  - [x] `getData(String assetId)` : lecture cache + refresh historique BD — **jamais** d'appel `PriceProvider`
  - [x] Utiliser `asset.dbSymbol()` pour **toutes** les requêtes JPA (ADR-03)
  - [x] Utiliser `asset.symbol()` uniquement pour le provider (via `PriceQuote` / FMP)
  - [x] Historique : throttle 1 h par actif sur refresh snapshots (pattern `InveBService.refreshHistory`)
  - [x] Live session : grouper `AssetDaily` par dernier jour ouvré dans la zone marché de l'actif
  - [x] Fallback : catch provider → log ERROR → cache existant avec `priceSource=CACHE`
- [x] Tests `ConfigurableAssetServiceTest` + `MarketHoursGuardTest` (AC: #6)
  - [x] Mockito : `AssetRegistry`, `PriceProviderRegistry`, repos — pas de `@SpringBootTest` obligatoire
  - [x] Réutiliser valeurs `InveBServiceTest` : prix `245.5`, marketCap `1_000_000_000`, change `-1.25`, volume `123_456`
- [x] `mvn test` vert — **ne pas** modifier `InveBService` ni `AssetSyncJob` dans cette story

## Dev Notes

### Story scope — IN / OUT

| IN scope | OUT of scope (stories suivantes) |
|----------|----------------------------------|
| `ConfigurableAssetService`, `MarketHoursGuard` | Refactor `InveBService` / `AssetSyncJob` (2.2, 4.5) |
| Shell `AssetDto` + enums `PriceSource` / `MarketStatus` | Contrat JSON complet + sérialisation (1.4) |
| Cache `AtomicReference` par `assetId` | `ProviderCallMetrics` (2.3) |
| Tests sync / fallback / market hours | `ScrapePriceProvider` (5.2) |
| Logique historique/live (copie comportement InveB) | Endpoint `GET /{assetId}` (1.5) |
| | Dividendes / fondamentaux YAML (Epic 3) |

**Ne pas toucher** `InveBService`, `AssetSyncJob`, `DashboardController` — le service générique est branché au job en Story 2.2 et à InveB en Story 4.5.

### Architecture compliance (ADR-01, ADR-03, ADR-10)

Package cible :

```text
backend/src/main/java/com/dokkcorp/dashboard/features/assets/
├── ConfigurableAssetService.java    # @Service
├── MarketHoursGuard.java            # @Component
└── model/
    ├── AssetDto.java                # shell — enrichi en 1.4
    ├── PriceSource.java
    └── MarketStatus.java
```

Signatures attendues (architecture §5) :

```java
@Service
public class ConfigurableAssetService {
    AssetDto getData(String assetId);      // lecture cache + historique BD — PAS de provider
    AssetDto syncPrice(String assetId);   // seul chemin d'appel PriceProvider
}

@Component
public class MarketHoursGuard {
    boolean isOpen(AssetDefinition asset);
    MarketStatus status(AssetDefinition asset);  // optionnel — dérivé de isOpen
}
```

**Séparation critique NFR-3 :**

| Méthode | Appelle provider ? | Écrit AssetDaily ? | Usage |
|---------|-------------------|-------------------|-------|
| `syncPrice` | Oui | Oui si marché ouvert | Job cron (2.2), tests, cold-start manuel |
| `getData` | **Non** | Non | Lecture HTTP future (1.5), lecture post-sync |

Si `getData` est appelé avec cache vide → retourner `AssetDto.error(...)` **sans** appeler FMP. Les tests peuvent appeler `syncPrice` d'abord pour peupler le cache.

### Mapping PriceQuote → cache → AssetDaily

| Champ AssetDto | Source sync réussie | Source fallback CACHE |
|----------------|--------------------|-----------------------|
| `assetId` | `asset.id()` | idem |
| `symbol` | `asset.dbSymbol()` | idem |
| `displayName` | `asset.displayName()` | idem |
| `type` | `asset.type()` | idem |
| `currency` | `quote.currency()` | depuis cache |
| `currentPrice` | `quote.price()` | depuis cache |
| `marketCap` | `quote.marketCap()` | depuis cache |
| `priceChangePercentage24h` | `quote.changePercent24h()` | depuis cache |
| `totalVolume` | `quote.volume()` | depuis cache |
| `lastRefresh` | `quote.fetchedAt().toEpochMilli()` | cache |
| `priceSource` | `PriceSource.FMP` | `PriceSource.CACHE` |
| `marketStatus` | `MarketHoursGuard.status(asset)` | idem |
| `history*` / `live*` | BD via `db-symbol` | idem |

Écriture `AssetDaily` (uniquement si `MarketHoursGuard.isOpen(asset)`):

```java
AssetDaily point = new AssetDaily();
point.setSymbol(asset.dbSymbol());           // "INVE-B" — PAS asset.symbol()
point.setCurrentPrice(quote.price());
point.setMarketCap(quote.marketCap());
point.setPriceChangePercentage24h(quote.changePercent24h());
point.setTotalVolume(quote.volume());
point.setLastRefresh(quote.fetchedAt());
assetDailyRepository.save(point);
```

### Résolution provider (Story 1.2)

```java
AssetDefinition asset = assetRegistry.findById(assetId)
    .orElseThrow(() -> new IllegalArgumentException("Unknown asset: " + assetId));

PriceProvider provider = priceProviderRegistry.requireById(asset.provider().providerId());
PriceQuote quote = provider.fetch(asset);
```

`AssetProvider.providerId()` existe déjà (`FMP` → `"fmp"`). Provider `scrape` lève `IllegalArgumentException` jusqu'à Epic 5 — acceptable.

### Logique historique / live (copier InveBService)

**Référence impérative** : `InveBService.java` lignes 69–180.

1. **Historique annuel** (`refreshHistory` équivalent par actif) :
   - `assetSnapshotRepository.findTop365BySymbolOrderByDayDesc(asset.dbSymbol())`
   - Inverser pour ordre chronologique croissant
   - Throttle : ne recharger que si `now - lastHistoryRefresh > 3_600_000` ms OU liste vide

2. **Session live intraday** :
   - `assetDailyRepository.findTop144BySymbolOrderByLastRefreshDesc(asset.dbSymbol())`
   - Déterminer `latestLocalDate` = date du point le plus récent dans `asset.marketHours().zone()`
   - Filtrer les points dont la date locale == `latestLocalDate`
   - Remplir `livePrices` / `liveDays` (epoch ms)

3. **Zone marché** : utiliser `asset.marketHours().zone()` — pas hardcoder `Europe/Stockholm` (prépare Story 2.1 multi-fuseaux).

### Cache par actif (NFR-6)

```java
private final ConcurrentHashMap<String, AtomicReference<AssetDto>> cacheByAssetId = new ConcurrentHashMap<>();
private final ConcurrentHashMap<String, Object> syncLocks = new ConcurrentHashMap<>();

private AtomicReference<AssetDto> cacheFor(String assetId) {
    return cacheByAssetId.computeIfAbsent(assetId, id -> new AtomicReference<>());
}
```

Pas de `loadLock` global unique — un verrou par `assetId` si double-checked locking nécessaire sur `syncPrice` (pattern BACK-24).

État historique volatile **par actif** (pas de champs globaux partagés entre inveb et futurs actifs).

### Shell AssetDto (Story 1.4 complètera le contrat)

Record minimal pour cette story — champs obligatoires pour cache/sync ; blocs métier null :

```java
public record AssetDto(
    String assetId,
    String symbol,              // db-symbol affiché
    String displayName,
    AssetType type,
    String currency,
    Double currentPrice,
    Double marketCap,
    Double priceChangePercentage24h,
    Double totalVolume,
    Long lastRefresh,
    PriceSource priceSource,
    MarketStatus marketStatus,
    List<Double> historyPrices,
    List<Long> historyDays,
    List<Double> livePrices,
    List<Long> liveDays,
    Object dividends,           // null — Epic 3
    Object fundamentals         // null — Epic 3
) {
    public static AssetDto error(String assetId, String dbSymbol) { ... }
}
```

Story 1.4 remplacera `Object` par `DividendsBlock` / `FundamentalsBlock` et ajoutera tests JSON. Ne pas bloquer 1.3 sur le contrat HTTP complet.

### MarketHoursGuard vs Story 2.1

Story 2.1 ajoutera des tests multi-fuseaux (London, NY, Zurich). **Cette story** crée `MarketHoursGuard` avec la logique générique depuis `MarketHours` — pas de logique marché inline dans `ConfigurableAssetService`.

Logique (équivalente à `InveBService` lignes 86–94) :

```java
ZonedDateTime now = ZonedDateTime.now(hours.zone());
DayOfWeek day = now.getDayOfWeek();
LocalTime time = now.toLocalTime();
return day != DayOfWeek.SATURDAY
    && day != DayOfWeek.SUNDAY
    && !time.isBefore(hours.open())
    && !time.isAfter(hours.close());
```

### Previous story intelligence (1.2)

- `FmpPriceProvider.fetch(asset)` — garde `asset.provider() == FMP`, délègue à `FMPClient`, `currency` depuis registre.
- `PriceProviderRegistry.requireById("fmp")` — lookup case-insensitive.
- `ExternalProviderException` remonte depuis `FMPClient` — **ne pas catch** dans le provider ; fallback cache dans `ConfigurableAssetService.syncPrice`.
- Tests WireMock dans `FmpPriceProviderTest` — réutiliser fixture `profile-inveb.json` via mock `PriceProvider` en unit test service (pas besoin de WireMock dans `ConfigurableAssetServiceTest`).
- Review 1.2 : test `fetch_rejectsNonFmpAsset`, test propagation exception sur réponse vide.

### Previous story intelligence (1.1)

- Registre prod : seule entrée `inveb` (`config/assets-registry.yml`).
- `findById` normalise lowercase — utiliser `"inveb"` dans les tests.
- `db-symbol: INVE-B` vs `symbol: INVE-B.ST` — **toujours** `dbSymbol()` pour JPA.

### File structure requirements

**NEW files :**

```text
backend/src/main/java/com/dokkcorp/dashboard/features/assets/
  ConfigurableAssetService.java
  MarketHoursGuard.java
  model/AssetDto.java
  model/PriceSource.java
  model/MarketStatus.java

backend/src/test/java/com/dokkcorp/dashboard/features/assets/
  ConfigurableAssetServiceTest.java
  MarketHoursGuardTest.java
```

**DO NOT MODIFY :**

```text
backend/src/main/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBService.java
backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java
backend/src/main/java/com/dokkcorp/dashboard/controller/DashboardController.java
backend/src/main/java/com/dokkcorp/dashboard/providers/fmp/FMPClient.java
```

### Testing requirements

Minimum **6 scénarios** dans `ConfigurableAssetServiceTest` :

| Test | Vérifie |
|------|---------|
| `syncPrice_persistsDailyAndUpdatesCache_whenMarketOpen` | AC #1 — save `AssetDaily` avec `db-symbol`, cache `priceSource=FMP` |
| `syncPrice_skipsDailyWrite_whenMarketClosed` | AC #5 — `never().save()` hors heures |
| `getData_returnsCacheWithoutProviderCall` | AC #2 — après sync, `getData` sans second `fetch` |
| `syncPrice_returnsStaleCacheOnProviderFailure` | AC #4 — `priceSource=CACHE`, prix inchangé |
| `syncPrice_returnsErrorWhenNoCacheAndProviderFails` | AC #4 — état dégradé |
| `getData_groupsLivePricesByLatestTradingDay` | AC #3 — reprend scénario `InveBServiceTest` |

`MarketHoursGuardTest` : samedi fermé, mardi 10:00 ouvert, mardi 18:00 fermé (zone Stockholm / entrée inveb).

- `mvn test` depuis `backend/` — tous tests existants + nouveaux verts.
- Pas de changement front (`@steve` uniquement).

### Anti-patterns à éviter

- **Ne pas** appeler `FMPClient` directement — passer par `PriceProviderRegistry`.
- **Ne pas** utiliser `asset.symbol()` pour les requêtes `AssetDaily` / `AssetSnapshot`.
- **Ne pas** refactorer `InveBService` pour déléguer — Story 4.5.
- **Ne pas** brancher `AssetSyncJob` sur `ConfigurableAssetService` — Story 2.2.
- **Ne pas** implémenter `computeEstimatedYield` — Epic 3.
- **Ne pas** créer `ScrapePriceProvider` ni enum `PriceSource.SCRAPE` avant Epic 5.
- **Ne pas** dupliquer la logique retry HTTP — reste dans `FMPClient` / providers.

### Latest technical notes

- **Java 21** + **Spring Boot 4** — `@Service` / `@Component` standard.
- Entités JPA `AssetDaily` / `AssetSnapshot` : champs `Double` (BACK-11 — pas de migration schéma).
- `Instant` en entité, epoch ms dans DTO (`toEpochMilli()`).
- Pas de Flyway — `ddl-auto: update` (NFR-11).

### Project Structure Notes

- Package `features/assets/model/` (singulier) — aligné code existant, pas `models/` de l'architecture doc.
- `ConfigurableAssetService` vit à la racine `features/assets/` (même niveau que `AssetRegistry`).
- Steve uniquement (`backend/**`) — pas de changement `frontend/**`.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-1.3]
- [Source: _bmad-output/planning-artifacts/architecture.md#ADR-01]
- [Source: _bmad-output/planning-artifacts/architecture.md#ADR-03]
- [Source: _bmad-output/planning-artifacts/architecture.md#ADR-10]
- [Source: _bmad-output/planning-artifacts/architecture.md#ADR-11]
- [Source: _bmad-output/planning-artifacts/architecture.md#ADR-15]
- [Source: _bmad-output/planning-artifacts/architecture.md#5-Interfaces-clés]
- [Source: backend/src/main/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBService.java]
- [Source: backend/src/test/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBServiceTest.java]
- [Source: _bmad-output/implementation-artifacts/stories/1-2-interface-priceprovider-et-fmppriceprovider.md]
- [Source: _bmad-output/implementation-artifacts/stories/1-1-registre-actifs-yaml-et-assetregistry.md]

## Dev Agent Record

### Agent Model Used

claude-4.6-sonnet-medium-thinking

### Debug Log References

### Completion Notes List

- Implemented `MarketHoursGuard` with injectable `Clock` for deterministic tests; weekend exclusion and inclusive open/close bounds aligned with `InveBService`.
- Added shell `AssetDto`, `PriceSource`, `MarketStatus` in `features/assets/model/`.
- Implemented `ConfigurableAssetService` with per-asset `AtomicReference` cache, per-asset history throttle (1h), live session grouping by latest trading day in asset zone, and provider fallback to stale cache (`PriceSource.CACHE`).
- `getData` never calls `PriceProvider`; returns `AssetDto.error` when cache is empty.
- 9 unit tests (6 service + 3 guard) — all pass via `mvnw test`.

### File List

- backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/MarketHoursGuard.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetDto.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/PriceSource.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/MarketStatus.java
- backend/src/test/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetServiceTest.java
- backend/src/test/java/com/dokkcorp/dashboard/features/assets/MarketHoursGuardTest.java

## Change Log

- 2026-06-18 — Story context créée (bmad-create-story)
- 2026-06-18 — Story 1.3 implemented: ConfigurableAssetService, MarketHoursGuard, AssetDto shell, unit tests (bmad-dev-story)
- 2026-06-18 — Code review patches appliqués : cache CACHE persisté, marketStatus live dans getData, test AC #4 (bmad-code-review)

### Review Findings

- [x] [Review][Patch] `fallbackFromCache` ne met pas à jour le cache avec `PriceSource.CACHE` [ConfigurableAssetService.java:113-119]
- [x] [Review][Patch] `getData` sert un `marketStatus` figé depuis le cache au lieu de `marketHoursGuard.status(asset)` [ConfigurableAssetService.java:64]
- [x] [Review][Patch] Test manquant : `getData` après échec provider doit exposer `PriceSource.CACHE` [ConfigurableAssetServiceTest.java]
- [x] [Review][Defer] Throttle historique contourné quand snapshots vides — comportement identique à `InveBService.refreshHistory` — deferred, pre-existing pattern
- [x] [Review][Defer] `MarketHoursGuard` ignore jours fériés — hors périmètre Story 1.3 / Story 2.1 — deferred, pre-existing
- [x] [Review][Defer] Week-ends fermés pour actifs 24/7 (crypto) — Epic futur, seul `inveb` en prod — deferred, pre-existing
