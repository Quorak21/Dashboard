---
story_id: 3.4
story_key: 3-4-alerte-fondamentaux-stale-et-endpoint-quarterly
epic: 3
epic_name: Dividendes, fondamentaux et alertes trimestrielles
status: review
baseline_commit: 35ff7e0
created: 2026-06-25
FRs:
  - FR-5
dependencies:
  - 3.2 (Configuration fondamentaux YAML)
  - 1.4 (Modèle AssetDto unifié)
---

# Story 3.4: Alerte fondamentaux stale et endpoint quarterly

Status: done

## Story

En tant que Dokk,
je veux être alerté quand les fondamentaux n'ont pas été mis à jour depuis 90 jours,
afin de planifier ma revue trimestrielle.

## Acceptance Criteria

### AC-1: Service d'alerte QuarterlyReportAlertService
**Given** `fundamentals.updatedAt` renseigné dans la configuration de fondamentaux
**When** `QuarterlyReportAlertService.getStaleAssets()` est appelé
**Then** il identifie les actifs dont les données ont été mises à jour il y a plus de 90 jours (valeur par défaut configurable via `app.alerts.fundamentals-stale-days`)
**And** il retourne la liste des alertes contenant `{ assetId, displayName, label, updatedAt, daysStale }`
**And** il utilise un `Clock` injectable pour pouvoir mocker la date actuelle dans les tests.

### AC-2: Exposition de l'endpoint d'alertes
**Given** des fondamentaux obsolètes pour certains actifs
**When** `GET /api/dashboard/alerts/quarterly` est appelé
**Then** il retourne un objet JSON de type `{ alerts: [...] }` contenant la liste des alertes de fondamentaux obsolètes
**And** la route est accessible publiquement sous `/api/dashboard/alerts/quarterly`.

### AC-3: Indicateur de fraîcheur dans AssetDto
**Given** le DTO d'un actif (`AssetDto`) assemblé par `ConfigurableAssetService`
**When** les fondamentaux sont inclus dans le bloc `fundamentals`
**Then** le champ `fundamentals.stale` (boolean) est évalué dynamiquement à `true` si le délai écoulé depuis `updatedAt` dépasse la limite configurée, sinon `false`.

### AC-4: Tests et seuils de validation
**Given** un seuil par défaut de 90 jours configuré
**When** les tests de validation sont exécutés
**Then** ils vérifient que :
- Un décalage de 89 jours n'est pas considéré comme stale (stale = false)
- Un décalage de 91 jours est considéré comme stale (stale = true)
- La route REST est correctement testée via `MockMvc` avec mock de `QuarterlyReportAlertService`.

## Tasks / Subtasks

- [x] Configuration de l'alerte
  - [x] Ajouter la propriété `app.alerts.fundamentals-stale-days: 90` dans `application.yml`
- [x] Créer les DTOs d'alerte
  - [x] Créer le record `StaleAssetAlert` pour encapsuler `{ assetId, displayName, label, updatedAt, daysStale }`
  - [x] Créer le record `QuarterlyAlertsResponse` contenant `List<StaleAssetAlert> alerts`
- [x] Implémenter le service QuarterlyReportAlertService
  - [x] Déclarer `QuarterlyReportAlertService` avec injecteur de `AssetRegistry`, `AssetFundamentalsProperties`, `Clock` et `staleDaysThreshold`
  - [x] Ajouter la méthode `isStale(LocalDate updatedAt)` comparant avec `LocalDate.now(clock)`
  - [x] Ajouter la méthode `getStaleAssets()` parcourant le registre et construisant les alertes pour les actifs obsolètes
- [x] Mettre à jour ConfigurableAssetService
  - [x] Injecter `QuarterlyReportAlertService` dans `ConfigurableAssetService`
  - [x] Mettre à jour `buildFundamentalsBlock` pour passer le résultat de `isStale` au constructeur de `FundamentalsBlock`
- [x] Exposer l'endpoint dans DashboardController
  - [x] Injecter `QuarterlyReportAlertService` dans `DashboardController`
  - [x] Ajouter la méthode avec `@GetMapping("/alerts/quarterly")` retournant `QuarterlyAlertsResponse`
- [x] Écrire les tests unitaires
  - [x] Écrire `QuarterlyReportAlertServiceTest` couvrant `isStale` et `getStaleAssets` (scénarios 89j, 90j, 91j, sans config, null updatedAt)
  - [x] Mettre à jour `ConfigurableAssetServiceTest` pour s'adapter à la modification du constructeur (mocking)
  - [x] Mettre à jour `DashboardControllerTest` pour tester l'endpoint d'alertes trimestrielles

## Dev Notes

### Architecture Patterns et Contraintes
- **Horodates et Fuseaux** : Toujours utiliser `LocalDate.now(clock)` avec le `Clock` fourni par Spring/injecté pour garantir la reproductibilité et la stabilité des assertions de tests (ADR-15).
- **Format JSON** : Respecter le format de l'endpoint et s'assurer que les dates sont correctement sérialisées (par exemple au format standard ISO `YYYY-MM-DD`).

### References
- [Contexte Projet](file:///c:/Users/chju/Desktop/Dashboard/docs/project-context.md)
- [Fiche d'Architecture (ADR-08)](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/architecture.md#L263)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium)

### Debug Log References

- Task ID: `7a581c1a-c006-4872-bc5c-b39b5afc0cdd/task-151` (mvn test pass)

### Completion Notes List

- Ajout de la propriété `app.alerts.fundamentals-stale-days: 90` dans la configuration.
- Création des DTOs/records `StaleAssetAlert` et `QuarterlyAlertsResponse`.
- Création du service d'alerte `QuarterlyReportAlertService` avec injecteur `@Autowired` et gestion de `Clock`.
- Intégration du service dans `ConfigurableAssetService` pour valoriser dynamiquement le statut `stale` des fondamentaux.
- Exposition de l'endpoint `@GetMapping("/alerts/quarterly")` dans `DashboardController`.
- Création du test unitaire `QuarterlyReportAlertServiceTest` (couvrant les cas limites de 89j vs 91j).
- Mise à jour des tests `ConfigurableAssetServiceTest` et `DashboardControllerTest` pour s'adapter aux modifications.

### File List

- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/alerts/StaleAssetAlert.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/alerts/QuarterlyAlertsResponse.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/alerts/QuarterlyReportAlertService.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java`
- `backend/src/main/java/com/dokkcorp/dashboard/controller/DashboardController.java`
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/alerts/QuarterlyReportAlertServiceTest.java`
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetServiceTest.java`
- `backend/src/test/java/com/dokkcorp/dashboard/controller/DashboardControllerTest.java`

### Review Findings

- [x] [Review][Decision] Clock injection design → resolved: `Clock` declared as `@Bean` in `ClockConfiguration`, injected via constructor. Dual-constructor pattern removed.
- [x] [Review][Decision] `label` field purpose → resolved: `label` = `asset.symbol()` (ticker symbol, distinct from `displayName`).
- [x] [Review][Patch] `getStaleAssets()` re-implements staleness logic inline → fixed: delegates to `isStale()`. [QuarterlyReportAlertService.java]
- [x] [Review][Patch] `Clock.systemDefaultZone()` uses JVM default timezone → fixed: `ClockConfiguration` provides `Clock.systemUTC()`. [ClockConfiguration.java]
- [x] [Review][Patch] `staleDaysThreshold` not validated → fixed: `Math.max(1, staleDaysThreshold)` guard added. [QuarterlyReportAlertService.java]
- [x] [Review][Patch] `assetRegistry.all()` not null-checked → fixed: null guard with warning log added. [QuarterlyReportAlertService.java]
- [x] [Review][Patch] Negative `daysStale` (future `updatedAt`) → fixed: guard with warning log, asset skipped. [QuarterlyReportAlertService.java]
- [x] [Review][Patch] `fmp-calls-warn-threshold: 200` dead configuration → fixed: removed from `application.yml`. [application.yml]
- [x] [Review][Patch] `getStaleAssets()` result not wrapped in `List.copyOf()` → fixed. [DashboardController.java]
- [x] [Review][Patch] Test boundary for `getStaleAssets()` at exactly 90 days missing → fixed: added `testGetStaleAssets_atExactly90Days_isNotStale`. [QuarterlyReportAlertServiceTest.java]
- [x] [Review][Patch] `application.yml` missing trailing newline → fixed. [application.yml]
- [x] [Review][Defer] YAML key case-sensitivity not enforced — silent lookup miss if YAML keys are mixed-case (pre-existing same issue in `buildFundamentalsBlock`) [QuarterlyReportAlertService.java] — deferred, pre-existing
- [x] [Review][Defer] `/alerts/quarterly` endpoint has no auth/access control — project-wide concern, no security layer exists anywhere in current codebase [DashboardController.java] — deferred, pre-existing
- [x] [Review][Defer] `LocalDate` serialization format not pinned (`@JsonFormat`) — controlled by project-wide Jackson config [StaleAssetAlert.java] — deferred, pre-existing
- [x] [Review][Defer] Silent skip of assets with `null` config/`updatedAt` — no warning log emitted (acceptable for YAML-driven config) — deferred, low priority
- [x] [Review][Defer] `ConfigurableAssetService` → `QuarterlyReportAlertService` coupling risk — no circular dependency today; Spring fails fast at startup — deferred, monitor
- [x] [Review][Defer] `isStale()` and `getStaleAssets()` compute `LocalDate.now()` independently — midnight race condition theoretical only in single-user personal tool — deferred, low risk
- [x] [Review][Defer] No pagination/size cap on alert endpoint — registry is small/static at current project scale — deferred, premature optimization
- [x] [Review][Defer] `getFundamentals()` called each invocation — Spring `@ConfigurationProperties` is in-memory, not expensive — deferred, not actionable
- [x] [Review][Defer] `QuarterlyAlertsResponse(null)` serialization risk — GlobalExceptionHandler already provides 500 fallback; service cannot return null — deferred, handled

## Change Log

- 2026-06-25: Implémentation du service d'alertes trimestrielles, de son endpoint REST et des tests (Dokk)
- 2026-06-25: Code review (3-layer adversarial): 2 decisions needed, 9 patches identified, 9 deferred (Dokk)
- 2026-06-25: All review patches applied and verified — 27 tests pass (Dokk)
