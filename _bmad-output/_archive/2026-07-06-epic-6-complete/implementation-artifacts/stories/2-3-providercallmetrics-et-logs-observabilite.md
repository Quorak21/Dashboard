---
story_id: 2.3
story_key: 2-3-providercallmetrics-et-logs-observabilite
epic: 2
epic_name: Synchronisation prix et observabilité
status: done
baseline_commit: 35ff7e0
created: 2026-06-23
FRs:
  - FR-18
NFRs:
  - NFR-4
dependencies:
  - 2.2 (AssetSyncJob tier FMP registry-driven)
  - Epic 1 (AssetRegistry, ConfigurableAssetService, MarketHoursGuard)
---

# Story 2.3 — ProviderCallMetrics et logs observabilité

## User Story

En tant que Dokk,
je veux voir en logs le nombre d'appels FMP et scrape du jour,
afin de surveiller les quotas (FR-18).

## Acceptance Criteria

### AC-1: FMP counter increments on each successful sync

**Given** `AssetSyncJob.syncFmpAssets()` boucle sur les actifs FMP du registre
**When** chaque actif FMP est synchronisé avec succès
**Then** `ProviderCallMetrics.incrementFmp()` est appelé pour chaque actif sync

### AC-2: Scrape counter increments on each successful scrape sync

**Given** `syncScrapeAssets()` s'exécute (future story, parallélisable avec AC-1)
**When** chaque actif scrape est synchronisé avec succès
**Then** `ProviderCallMetrics.incrementScrape()` est appelé

### AC-3: Hourly log INFO with call counts

**Given** des sync FMP et scrape exécutées dans l'heure
**When** une heure s'écoule ou à chaque fin de sync batch
**Then** log INFO `FMP calls today: N` et `Scrape calls today: M`

### AC-4: WARN log when FMP quota approaches limit

**Given** le compteur FMP > 200 (seuil `app.alerts.fmp-calls-warn-threshold`)
**When** `ProviderCallMetrics.logMetrics()` est appelé
**Then** log WARN `⚠ FMP calls today: N — approaching daily limit`

**And** le seuil est configurable dans `application.yml` via `app.alerts.fmp-calls-warn-threshold: 200`

### AC-5: WARN log when scrape failure rate exceeds 20%

**Given** un historique d'appels scrape avec taux d'échec > 20 % sur la journée UTC (compteurs journaliers en mémoire)
**When** `ProviderCallMetrics.logMetrics()` est appelé
**Then** log WARN `⚠ Scrape failure rate: X% — above 20% threshold`

### AC-6: Counters reset at midnight UTC

**Given** minuit UTC atteint
**When** un nouveau compteur est incrémenté
**Then** les compteurs FMP et scrape repartent de zéro

**And** le reset est basé sur `LocalDate` (date UTC courante) — pas de cron de reset

### AC-7: Stale cache WARN logs

**Given** `ConfigurableAssetService.syncPrice(assetId)` échoue et tombe en fallback cache
**When** le cache stale est renvoyé
**Then** log WARN `Prix stale pour {assetId}, source=cache, age={X}min`

### AC-8: Scrape failure tracking

**Given** `syncPrice` échoue pour un actif scrape (HTTP ou parse exception)
**When** l'exception est catchée
**Then** `ProviderCallMetrics.recordScrapeFailure()` est appelé

**And** un succès scrape appelle `recordScrapeSuccess()` ou `incrementScrape()` (selon design retenu)

### AC-9: Unit test ProviderCallMetricsTest

**Given** `ProviderCallMetrics` instancié
**When** les compteurs sont incrémentés et `logMetrics()` appelé
**Then** les logs contiennent les valeurs attendues
**And** test vérifie reset à minuit
**And** test vérifie WARN FMP > 200
**And** test vérifie WARN échec scrape > 20 %

---

## Tasks / Subtasks

- [x] Create `ProviderCallMetrics` bean with FMP/scrape counters, day reset, and `logMetrics()`
- [x] Add `app.alerts.fmp-calls-warn-threshold` to `application.yml` (main + test)
- [x] Integrate `incrementFmp()` and `logMetrics()` in `AssetSyncJob.syncFmpAssets()`
- [x] Add stale cache WARN and scrape metrics in `ConfigurableAssetService.syncPrice()`
- [x] Add `ProviderCallMetricsTest` (counters, INFO logs, WARN thresholds, UTC reset)
- [x] Update `AssetSyncJobTest` and `ConfigurableAssetServiceTest` for new dependencies
- [x] Run full Maven test suite — all 74 tests pass

### Review Findings

- [x] [Review][Decision] FMP counter increments on stale-cache fallback — résolu : `incrementFmp()` déplacé dans `ConfigurableAssetService` uniquement après fetch FMP réussi (quote valide).
- [x] [Review][Decision] Scrape failure rate uses daily cumulative counters — résolu : AC-5 et ADR-12 alignés sur cumul journalier UTC.
- [x] [Review][Patch] Scrape failure-rate denominator excludes failed attempts — résolu : chaque échec scrape appelle `incrementScrape()` + `recordScrapeFailure()`.
- [x] [Review][Patch] Null-quote SCRAPE path records neither success nor failure — résolu.
- [x] [Review][Patch] Null FMP registry skips metrics heartbeat — résolu : `logMetrics()` toujours appelé en fin de batch.
- [x] [Review][Patch] ConfigurableAssetServiceTest lacks scrape/stale metric assertions — résolu : 4 tests ajoutés.
- [x] [Review][Patch] UTC day-reset race under concurrent increments — résolu : `synchronized(dayResetLock)` sur reset et incréments.
- [x] [Review][Defer] FMP HTTP calls outside AssetSyncJob not counted (InveB legacy path) [pre-existing] — deferred, out of story 2.3 scope
- [x] [Review][Defer] Scrape 20% threshold hardcoded while FMP threshold is configurable [ProviderCallMetrics.java:17] — deferred, not required by AC-4
- [x] [Review][Defer] No Micrometer/Prometheus metrics, SLF4J only [ProviderCallMetrics.java] — deferred, ADR-12 log-based observability is sufficient for now
- [x] [Review][Defer] Scrape metrics emitted only when FMP batch runs [AssetSyncJob.java:92] — deferred until `syncScrapeAssets` batch exists (AC-2 future story)

---

## Dev Agent Record

### Implementation Plan

1. `ProviderCallMetrics` — thread-safe `AtomicLong` counters, UTC day reset via `LocalDate`, configurable FMP warn threshold via `@Value` field injection (no-arg constructor for Spring Boot 4 compatibility).
2. `AssetSyncJob` — `incrementFmp()` after each successful open-market sync; `logMetrics()` at end of FMP batch.
3. `ConfigurableAssetService` — `incrementScrape()` on scrape success, `recordScrapeFailure()` on scrape exception, stale cache WARN with age in minutes.
4. Tests — Logback `ListAppender` for log assertions; `createForTest()` factory for clock injection.

### Debug Log

- Spring Boot 4 failed to instantiate bean with `@Value` constructor + package-private test constructor (`No default constructor found`). Resolved by switching to no-arg constructor + `@Value` field injection.

### Completion Notes

- All 9 acceptance criteria satisfied.
- `mvnw test` — 74 tests, 0 failures.
- Scrape counter integration is in `ConfigurableAssetService` (ready for Epic 5 `syncScrapeAssets`); FMP counter in `AssetSyncJob` per AC-1.

---

## File List

- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/ProviderCallMetrics.java` (new)
- `backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java` (modified)
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java` (modified)
- `backend/src/main/resources/application.yml` (modified)
- `backend/src/test/resources/application.yml` (modified)
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/ProviderCallMetricsTest.java` (new)
- `backend/src/test/java/com/dokkcorp/dashboard/jobs/AssetSyncJobTest.java` (modified)
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetServiceTest.java` (modified)

---

## Change Log

- 2026-06-23: Code review — FMP compté sur succès réel, dénominateur scrape corrigé, sync null-registry, race UTC, tests enrichis.
- 2026-06-23: Implemented FR-18 observability — `ProviderCallMetrics` bean, FMP batch logging in `AssetSyncJob`, stale cache WARN and scrape tracking in `ConfigurableAssetService`, unit tests.

---

## Developer Context

### Architecture Compliance

- **ADR-12 (Observabilité providers)**: Bean `ProviderCallMetrics` unique, compteurs journaliers reset minuit UTC, log INFO en fin de batch FMP, WARN si FMP > 200/j ou scrape failure > 20% (cumul journalier en mémoire)
- **Package** (architecture §4): `com.dokkcorp.dashboard.features.assets.ProviderCallMetrics`
- **Reset**: basé sur `LocalDate.now(ZoneOffset.UTC)` — pas besoin de `@Scheduled`

### Technical Requirements

#### ProviderCallMetrics.java — design spec

```java
package com.dokkcorp.dashboard.features.assets;

@Component
public class ProviderCallMetrics {

    private static final Logger logger = LoggerFactory.getLogger(ProviderCallMetrics.class);

    // Seuil configurable via app.alerts.fmp-calls-warn-threshold
    private final int fmpWarnThreshold;

    // AtomicLongs pour compteurs thread-safe (sync jobs run on scheduler threads)
    private final AtomicLong fmpCalls = new AtomicLong(0);
    private final AtomicLong scrapeCalls = new AtomicLong(0);
    private final AtomicLong scrapeFailures = new AtomicLong(0);

    // LcoalDate tracking pour reset
    private volatile LocalDate currentDay;

    public ProviderCallMetrics(@Value("${app.alerts.fmp-calls-warn-threshold:200}") int fmpWarnThreshold) {
        this.fmpWarnThreshold = fmpWarnThreshold;
        this.currentDay = LocalDate.now(ZoneOffset.UTC);
    }

    private void checkDayReset() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (!today.equals(currentDay)) {
            fmpCalls.set(0);
            scrapeCalls.set(0);
            scrapeFailures.set(0);
            currentDay = today;
        }
    }

    public void incrementFmp() {
        checkDayReset();
        fmpCalls.incrementAndGet();
    }

    public void incrementScrape() {
        checkDayReset();
        scrapeCalls.incrementAndGet();
    }

    public void recordScrapeFailure() {
        checkDayReset();
        scrapeFailures.incrementAndGet();
    }

    public void logMetrics() {
        checkDayReset();
        long fmp = fmpCalls.get();
        long scrape = scrapeCalls.get();
        long scrapeFails = scrapeFailures.get();

        logger.info("FMP calls today: {}", fmp);
        logger.info("Scrape calls today: {}", scrape);

        if (fmp > fmpWarnThreshold) {
            logger.warn("⚠ FMP calls today: {} — approaching daily limit", fmp);
        }

        if (scrape > 0) {
            double failRate = (double) scrapeFails / scrape * 100;
            if (failRate > 20.0) {
                logger.warn("⚠ Scrape failure rate: {}% — above 20% threshold",
                        String.format("%.1f", failRate));
            }
        }
    }

    // Exposed for tests
    long fmpCalls() { return fmpCalls.get(); }
    long scrapeCalls() { return scrapeCalls.get(); }
    long scrapeFailures() { return scrapeFailures.get(); }
    LocalDate currentDay() { return currentDay; }
}
```

#### Stale cache WARN in ConfigurableAssetService

In `ConfigurableAssetService.fallbackFromCache()`, add a WARN log when returning stale cache:

```java
if (stale != null) {
    long ageMinutes = ...; // calcul depuis lastRefresh
    logger.warn("Prix stale pour {}, source=cache, age={}min", assetId, ageMinutes);
    ...
}
```

#### Integration in AssetSyncJob

In `syncFmpAssets()`, after `configurableAssetService.syncPrice(id)`, inject `ProviderCallMetrics.incrementFmp()`:

```java
if (this.marketHoursGuard.isOpen(asset)) {
    this.configurableAssetService.syncPrice(asset.id());
    this.providerCallMetrics.incrementFmp();
}
```

Also call `providerCallMetrics.logMetrics()` at the end of each sync batch (log INFO + potential WARN).

#### Stale cache age calculation

```java
private long computeStaleAgeMinutes(AssetDto cached) {
    if (cached.lastRefresh() == null) return 0;
    return ChronoUnit.MINUTES.between(
            Instant.ofEpochMilli(cached.lastRefresh()),
            Instant.now());
}
```

### File Structure Requirements

**New files:**
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/ProviderCallMetrics.java`

**Modified files:**
- `backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java` — inject ProviderCallMetrics, increment after sync

**New test files:**
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/ProviderCallMetricsTest.java`

**Modified config:**
- `backend/src/main/resources/application.yml` — add `app.alerts.fmp-calls-warn-threshold: 200`
- `backend/src/test/resources/application.yml` — mirror the config

### Testing Requirements

#### ProviderCallMetricsTest

1. **Counters increment**: incrementFmp/incrementScrape → counters match
2. **Log metrics**: capture log output, verify INFO lines contain "FMP calls today" and "Scrape calls today"
3. **WARN FMP threshold**: increment FMP > 200 times, call logMetrics → verify WARN
4. **WARN scrape failure**: increment scrape + record failures ratio > 20% → verify WARN
5. **Reset at midnight**: simulate day change (set currentDay to yesterday) → next increment resets counters
6. **No WARN below threshold**: FMP=100, scrape=0 fails → no WARN

Use `LoggerFactory` + `org.slf4j.Logger` with a `ListAppender` / `MemoryAppender` utility pattern to capture log output.

#### AssetSyncJobTest update

- Add `providerCallMetrics` mock to test
- Verify `incrementFmp()` is called after each sync in `syncFmpAssets` tests

### Previous Story Intelligence (2.2)

- **Story 2.2** créé `syncFmpAssets()` dans `AssetSyncJob` avec itération registre, MarketHoursGuard, isolation try/catch par actif.
- L'injection `ConfigurableAssetService` via constructeur est déjà en place.
- Les tests existants vérifient : sync actifs ouverts OK, sync actifs fermés skip, isolation échec par actif, liste nulle.
- **Leçon**: AssetSyncJobTest utilise Mockito direct (sans Spring Boot test) — cohérent, continuer ce pattern pour `ProviderCallMetricsTest`.

### Git Intelligence Summary

- Les commits récents (10 derniers) sont sur le frontend HYPE (chart fix, UI bugs, volume/OI charts) — pas de pattern backend supplémentaire à suivre.
- Les classes backend sont stabilisées depuis Epic 1 et les premiers commits de la session actuelle.

### Library / Framework Requirements

- `java.util.concurrent.atomic.AtomicLong` — thread-safe compteurs
- `java.time.LocalDate`, `java.time.ZoneOffset` — reset midnight
- `org.slf4j.Logger` / `LoggerFactory` — logging
- Spring `@Value` — configuration threshold

### Application Config

Add to `application.yml`:

```yaml
app:
  alerts:
    fmp-calls-warn-threshold: 200
```

### Project Context Reference

- **Steve** (backend only) — ce story est 100% backend.
- `AssetSyncJob` (à modifier) vit dans `jobs/`.
- Nouveau bean `ProviderCallMetrics` dans `features/assets/` cohérent avec ADR-12.
- Stale cache WARN dans `ConfigurableAssetService.fallbackFromCache()` déjà en `features/assets/`.
- L'ADR-12 mentionne aussi un WARN sur lecture cache stale — à implémenter dans `ConfigurableAssetService`.

---

## Story Completion Status

**Status**: done  
**Notes**: Code review appliqué — FMP compté sur succès réel, dénominateur scrape corrigé, tests enrichis, 79 tests passent.
