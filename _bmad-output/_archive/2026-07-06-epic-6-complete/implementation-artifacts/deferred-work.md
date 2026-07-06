## Deferred from: code review of 1-3-configurableassetservice-cache-sync-et-historique.md (2026-06-18)

- Throttle historique contourné quand snapshots vides — comportement identique à `InveBService.refreshHistory`
- `MarketHoursGuard` ignore jours fériés — hors périmètre Story 1.3 / Story 2.1
- Week-ends fermés pour actifs 24/7 (crypto) — Epic futur, seul `inveb` en prod

## Deferred from: code review of 1-4-modele-assetdto-unifie.md (2026-06-18)

- Concurrency/Performance bottleneck: Holding synchronisation lock during external `provider.fetch` network call (ConfigurableAssetService.java:70).
- Unbounded cache maps: Concurrent hash maps (`syncLocks`, `cacheByAssetId`, `historyStateByAssetId`) grow indefinitely without eviction/expiration policy.
- Unsynchronized `refreshHistory` database race: Concurrent requests to `getData` can query the database concurrently and cause write races on state fields.
- Suboptimal Live Series SQL query: In-memory filtering instead of query filtering by date (ConfigurableAssetService.java:165).
- Hardcoded JPA query limit names: Repository method names hardcode numeric query limits (e.g. `findTop144...`, `findTop365...`).
- DB Write exception handling: `persistDailyPoint` doesn't have transaction bounds or exception handling.
- Reversing live series in memory: SQL query OrderByLastRefreshDesc reversed in memory rather than sorting Ascending at database level.

## Deferred from: code review of 2-1-markethoursguard-multi-fuseaux.md (2026-06-18)

- `AssetDto` Java sans miroir TypeScript — différé à Story 4-1 (Epic 4). Créer le modèle TS sans composant front serait prématuré.
- Race condition non atomique sur `historyPrices`/`historyDays` dans `refreshHistory` — patterns établis dans le projet, refactor Clock planifié.
- Interception globale de `Exception` dans `syncPrice` masque les bugs internes — pré-existant, refactoring à planifier.
- `System.currentTimeMillis()` rend les tests d'expiration non déterministes — refactoring Clock à planifier.
- Duplication de `db-symbol` entre actifs non détectée dans `DefaultAssetRegistry` — faible risque immédiat avec peu d'actifs.
- Instabilité de verrous sur rechargement du registre dans `synchronizeCachesWithRegistry` — rechargement à chaud non implémenté.
- `getData` renvoie un DTO erreur au démarrage à froid avant premier run du scheduler — UX acceptable pour l'instant.
- Actifs `brwm`, `o`, `chdiv` absents du registre de production — prévu dans Epics 5 et 6.
- Package `model` (singulier) vs convention docs `models` (pluriel) — incohérence mineure à harmoniser à terme.


## Deferred from: code review of 2-2-assetsyncjob-tier-fmp-registry-driven.md (2026-06-23)

- Sequential External API calls blocking Scheduled Task Executor: Calling external API syncs sequentially inside @Scheduled tasks using the default single-threaded scheduled executor can block execution of other tasks (AssetSyncJob.java:71-82).
- Lack of rate limiting/throttling for external calls: Loop triggers calls without any delay, potentially hitting FMP rate limits if there are many assets (AssetSyncJob.java:71-82).

## Deferred from: code review of 2-4-snapshots-quotidiens-pour-actifs-registre.md (2026-06-24)

- Performance risk (N+1 queries) during registry snapshot loop (AssetSyncJob.java:97).
- No automated scheduling for SCRAPE provider assets (AssetSyncJob.java:69).
- Scheduled sync ignores YAML registry interval and offset configs (AssetSyncJob.java:69).
- Risk of NullPointerException in MarketHoursGuard.isOpen when stubbing in tests (MarketHoursGuard.java:1).
- Sequential blocking network calls in scheduled tasks (AssetSyncJob.java:69).
- Holding synchronisation lock during external provider.fetch network call (ConfigurableAssetService.java:1).
- Absence of limitation of debit on external FMP calls (AssetSyncJob.java:69).


## Deferred from: code review of 3-2-configuration-fondamentaux-yaml.md (2026-06-25)

- Synchronous DB query on read requests (ConfigurableAssetService.java:360)
- Thread-safety and Lock Replacement Risks in synchronizeCachesWithRegistry (ConfigurableAssetService.java)
- Unit tests coupled to production files (AssetFundamentalsPropertiesTest.java)
- Redundant AtomicReference wrapper in cache (ConfigurableAssetService.java)
- Stale Cache Age calculation bug (ConfigurableAssetService.java:443)

## Deferred from: code review of 3-3-calcul-estimatedyield-backend.md (2026-06-25)

- [Review][Defer] I/O réseau bloquante dans le bloc synchronized de syncPrice (ConfigurableAssetService.java:122)
- [Review][Defer] Utilisation de la réflexion dans les tests unitaires (ConfigurableAssetServiceTest.java:18)
- [Review][Defer] Mises à jour non atomiques des champs volatiles dans HistoryState (ConfigurableAssetService.java:413)
- [Review][Defer] Suivi incohérent des métriques d'échec des providers dans syncPrice (ConfigurableAssetService.java:179)
- [Review][Defer] Utilisation de sources de temps incohérentes (ConfigurableAssetService.java:207)
- [Review][Defer] Conditions de concurrence lors de l'éviction dynamique du cache (ConfigurableAssetService.java:93)
- [Review][Defer] Requêtes base de données lors d'opérations en lecture seule (ConfigurableAssetService.java:111)
- [Review][Defer] Risque de NullPointerException dans les builders de blocs de propriétés (ConfigurableAssetService.java:335)
- [Review][Defer] Absence de garde pour dailyPoints null retourné par le repository (ConfigurableAssetService.java:261)
- [Review][Defer] Absence de garde contre les éléments null dans la liste des points quotidiens (ConfigurableAssetService.java:283)
- [Review][Defer] Risque de NumberFormatException lors de la conversion de prix Double.NaN/Infinite (ConfigurableAssetService.java:348)
- [Review][Defer] Risque de NullPointerException si la map de métriques contient des valeurs null (ConfigurableAssetService.java:385)
- [Review][Defer] Absence de garde contre les ID d'actifs null dans le registre (ConfigurableAssetService.java:93)

## Deferred from: code review of 3-4-alerte-fondamentaux-stale-et-endpoint-quarterly.md (2026-06-25)

- YAML key case-sensitivity not enforced in `QuarterlyReportAlertService` — silent miss if YAML keys are mixed-case (same pre-existing issue in `buildFundamentalsBlock`)
- `/alerts/quarterly` endpoint has no authentication/access control — project-wide concern; no security layer exists in current codebase
- `LocalDate updatedAt` serialization format not pinned with `@JsonFormat` — controlled by project-wide Jackson config; consistent with all other DTOs
- Silent skip of assets with `null` fundamentals config or `null updatedAt` — no warning log; acceptable for YAML-driven static config
- `ConfigurableAssetService` → `QuarterlyReportAlertService` coupling risk — no circular dependency today; Spring fails fast at startup if introduced
- `isStale()` and `getStaleAssets()` compute `LocalDate.now()` independently (midnight race) — theoretical only in single-user personal tool
- No pagination or size cap on `/alerts/quarterly` — registry is small and static at current project scale
- `getFundamentals()` called on every `getStaleAssets()` invocation — Spring `@ConfigurationProperties` is in-memory; cost is negligible
- `QuarterlyAlertsResponse(null)` would serialize as `{"alerts":null}` — `GlobalExceptionHandler` covers the service-throw case; service cannot return null in practice

## Deferred from: code review of 3-5-fichiers-yaml-metier-pour-les-7-actifs.md (2026-06-25)

- Représentation incohérente des devises (codes ISO vs symboles) : Les fichiers de dividendes utilisent les codes ISO tandis que les fondamentaux utilisent des symboles/formats mélangés.
- Dépendance des tests unitaires envers les fichiers de données réels : Le test `AssetFundamentalsPropertiesTest` fait des assertions sur les fichiers réels.
- Typage faible des métriques fondamentales : Le champ `metrics` est de type `Map<String, Object>` (conception validée dans la Story 3.2 pour flexibilité).

## Deferred from: code review of 4-1-modele-assetdto-front-et-dashboardapiservice.md (2026-06-30)

- Partial DTO mocking in tests using `as AssetDto` (frontend/src/app/core/services/dashboard-api.service.spec.ts:40)
- Non-standard filename for `toastService.ts` (frontend/src/app/core/services/toastService.ts:1)

## Deferred from: code review of 4-2-composants-generiques-dividendcard-et-fundamentalscard.md (2026-06-30)

- Non-localized Hardcoded Currency Format: Formats currency dynamically via custom string concat `${value} ${currency}`, which does not conform to localization rules (where currency symbol can prefix value).

## Deferred from: code review of 4-3-template-assetpage-et-pricefreshnessbadge.md (2026-06-30)

- [Review][Defer] F5: Titres de graphiques en anglais ("Annual Performance", "Daily Live") dans une UI en français — cohérence cosmétique, non bloquant (asset-page.html:43,55)
- [Review][Defer] F6: `staleThresholdMs` exposé en tant qu'`input()` public sur `PriceFreshnessBadge` — devrait être une constante interne ou un input protégé (price-freshness-badge.ts:16)

## Deferred from: code review of 4-6-carte-overview-inveb-et-bandeau-alertes.md (2026-07-01)

- Data Fetching in Constructor: Data fetching is initiated in the constructor via `this.refresh()` rather than in an Angular lifecycle hook like `ngOnInit`. (frontend/src/app/features/dashboard/dashboard.ts:32-36)

## Deferred from: code review of 6-1-blackrock-world-mining-brwm.md (2026-07-01)

- Highly Hardcoded Asset Rendering in Dashboard: Adding the `brwm` asset requires duplicating code across the dashboard template, signals, and manual subscriptions. A dynamic rendering using `@for` loop over registered assets should be implemented in the future.
- Hardcoded UI Currency Symbols in Template: The currency symbols for the dashboard cards are hardcoded directly in the HTML template.
- Mixed French/English in Unit Tests: `dashboard.spec.ts` has French variables and test descriptions, while the rest of the codebase is in English.
- Silent Coercion of Missing Numeric Values to Zero: Jackson deserializes missing primitive double fields (like `marketCap`) to `0.0` silently without validation.

## Deferred from: code review of 6-2-realty-income-o.md (2026-07-01)

- Duplicated assets-registry configuration: The configuration file `backend/src/main/resources/config/assets-registry.yml` is duplicated verbatim in `backend/src/test/resources/config/assets-registry.yml`.
- Severe copy-paste anti-pattern in frontend components: Separate folder, component class, HTML template, and test file created for each stock symbol (e.g., `inveb`, `brwm`, `o`) instead of using parameterized routing.
- Hardcoded route definitions: Routing paths for each stock path are manually defined/hardcoded in `app.routes.ts`.
- Inconsistent data types in YAML files: `fundamentals/o.yml` uses string representations for percentages/leverage, while PE ratios are numbers.
- Repetitive subscription boilerplate in dashboard.ts: API subscriptions are repeated block-by-block for each asset in `dashboard.ts`.

## Deferred from: code review of 6-3-3i-group-iii.md (2026-07-02)

- Duplicate Registry Configuration Files: The configuration file `assets-registry.yml` is duplicated between main resources and test resources. This increases maintenance risk when setting new properties.
- Configuration Mismatch in Test Data for brwm: The sync offset for the `brwm` asset is defined as 3 minutes in the YAML registry, but the static test definition hardcodes it to 0 (`new SyncConfig(15, 0)`).
- Severe Angular Component and Route Duplication: Separate standalone component wrappers and routes are created for each asset (`inveb`, `brwm`, `o`, `iii`). The routes and detail pages should ideally be generic and parameterized (e.g. using a `:assetId` route parameter).
- Repetitive Manual Subscription Management on Dashboard: Boilerplate subscription management (`invebSub`, `brwmSub`, `oSub`, `iiiSub`) is used for each asset, which increases boilerplate code and memory leak risks compared to declarative RxJS streams.
- Hardcoded UI Labels and Currency Symbols on Dashboard: Display titles and currency symbols are hardcoded in `dashboard.html` rather than being provided dynamically via the API (which already carries registry metadata).
- Redundant Global Mocking of ResizeObserver: `ResizeObserver` is mocked in the local component spec file. This should be configured globally in a central test setup file.
- Inconsistent Test Fixture Retrieval Strategy: Tests mix loading mock profiles from JSON files (e.g. `profile-brwm.json`) and inline JSON string literals.
- Inconsistent Data Scale in Mock Objects: Mock data fields for volume and market cap are downscaled compared to the JSON file fixture (`marketCap: 30000` vs `30000000000`).

## Deferred from: code review of 6-4-ishares-global-infrastructure-infr-et-etfsectorchart.md (2026-07-02)

- Severe Angular Component and Route Duplication: Standalone component wrappers and hardcoded routes created for the 'infr' asset instead of using parameterized routing (e.g. '/stocks/:assetId').

## Deferred from: code review of 6-5-overview-complet-et-section-etf-active.md (2026-07-02)

- Copy-paste explosion in `refresh()` — 5 structurally identical `getAsset` subscription blocks with no abstraction; consider a data-driven Map<string, WritableSignal<AssetDto|null>> approach [dashboard.ts:66-114]
- Stale in-flight HTTP response race — `unsubscribe()` before resubscribe does not cancel the XHR at network level; stale response can still overwrite signal state [dashboard.ts:67-113]
- Sequential unsubscribe in `onDestroy` — exception in first `unsubscribe()` would abort cleanup of remaining 5 subs; consider wrapping in try-finally [dashboard.ts:55-63]
- 6 simultaneous API errors collapse to 1 toast — the toast service clears its timer on each call so only the last error message is shown [dashboard-api.service.ts:18-23]
- No test coverage for the error path (subscribe error → signal reset to null) or the 3-minute refresh interval [dashboard.spec.ts]
- magic string asset IDs ('inveb', 'brwm', 'o', 'iii', 'infr') scattered across component, service, and tests — no shared enum/constant; typo silently returns null data [dashboard.ts]

## Deferred from: code review of 6-6-navigation-complete-et-nettoyage-legacy.md (2026-07-06)

- Non-standard filename for `toastService.ts` on disk (frontend/src/app/core/services/toastService.ts:1).
- Weak test coverage for `testGetLastHypeData` asserting only status 200 without payload checks (backend/src/test/java/com/dokkcorp/dashboard/controller/DashboardControllerTest.java:52).
