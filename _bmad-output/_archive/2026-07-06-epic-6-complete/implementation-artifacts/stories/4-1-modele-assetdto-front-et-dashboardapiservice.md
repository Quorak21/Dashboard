---
story_id: 4.1
story_key: 4-1-modele-assetdto-front-et-dashboardapiservice
epic: 4
epic_name: UI générique et migration Investor AB
status: done
baseline_commit: 35ff7e0
created: 2026-06-30
FRs:
  - FR-2
  - FR-14
dependencies:
  - 3.4 (Alerte fondamentaux stale et endpoint quarterly)
  - 3.5 (Fichiers YAML métier pour les 7 actifs)
---

# Story 4.1: Modèle AssetDto front et DashboardApiService

Status: done


## Story

En tant que Dokk,
je veux les types TypeScript miroir du backend et un client API mis à jour,
afin de consommer l'API unifiée d'actifs et les alertes trimestrielles depuis Angular.

## Acceptance Criteria

### AC-1: Modèle TypeScript miroir du backend
**Given** les records Java constituant le DTO d'actif (`AssetDto`, `DividendsBlock`, `FundamentalsBlock`, etc.)
**When** le frontend TypeScript compile
**Then** le fichier `frontend/src/app/core/models/asset.dto.ts` définit des interfaces TypeScript équivalentes et typées de façon stricte.
**And** les enums `AssetType` ('STOCK' | 'REIT' | 'ETF' | 'TRUST'), `PriceSource` ('FMP' | 'SCRAPE' | 'CACHE') et `MarketStatus` ('OPEN' | 'CLOSED') sont typés.
**And** les interfaces `StaleAssetAlert` et `QuarterlyAlertsResponse` sont également définies pour représenter l'API d'alertes.
**And** toutes ces interfaces sont proprement exportées via `frontend/src/app/core/models/index.ts`.

### AC-2: Client d'API DashboardApiService étendu
**Given** le service d'API `DashboardApiService`
**When** `getAsset(assetId)` est appelé avec un identifiant d'actif (ex. `'brwm'`, `'o'`, `'inveb'`)
**Then** il effectue une requête HTTP `GET /api/dashboard/asset/{assetId}` et retourne un `Observable<AssetDto | null>`.
**And** en cas d'erreur HTTP (ex. 404 ou 500), il affiche un toast français via `ToastService` (« Erreur de connexion avec le serveur, veuillez réessayer plus tard ») et retourne un flux émettant `null` (conformément à la règle NFR-9 du projet).
**And** la méthode `getQuarterlyAlerts()` est ajoutée pour appeler `GET /api/dashboard/alerts/quarterly` et retourne un `Observable<QuarterlyAlertsResponse | null>` avec la même gestion d'erreur.

### AC-3: Tests unitaires du client API
**Given** `DashboardApiService`
**When** les tests Vitest sont exécutés
**Then** ils vérifient via `HttpTestingController` :
- Le bon appel et le bon mapping en succès pour `getAsset(assetId)`
- Le bon appel et le bon mapping en succès pour `getQuarterlyAlerts()`
- Le comportement en cas d'erreur HTTP (affichage du toast et retour de `null`) pour les deux méthodes.

## Tasks / Subtasks

- [x] Modèles TypeScript DTO (AC-1)
  - [x] Créer le fichier [asset.dto.ts](file:///frontend/src/app/core/models/asset.dto.ts)
  - [x] Y déclarer les types/interfaces :
    - `AssetType` ('STOCK' | 'REIT' | 'ETF' | 'TRUST')
    - `PriceSource` ('FMP' | 'SCRAPE' | 'CACHE')
    - `MarketStatus` ('OPEN' | 'CLOSED')
    - `DividendHistoryEntry` `{ year: number; amount: number; currency: string; }`
    - `DividendsBlock` `{ forwardDividend: number; forwardDividendCurrency: string; frequency: string; estimatedYield?: number; avgDividendGrowth10Y?: number; history: DividendHistoryEntry[]; }`
    - `HoldingEntry` `{ name: string; weightPercent: number; }`
    - `SectorWeight` `{ sector: string; weightPercent: number; }`
    - `FundamentalsBlock` `{ updatedAt: string; source: string; stale: boolean; metrics: Record<string, any>; topHoldings?: HoldingEntry[]; sectorWeights?: SectorWeight[]; }`
    - `AssetDto`
    - `StaleAssetAlert` `{ assetId: string; displayName: string; label: string; updatedAt: string; daysStale: number; }`
    - `QuarterlyAlertsResponse` `{ alerts: StaleAssetAlert[]; }`
  - [x] Mettre à jour [index.ts](file:///frontend/src/app/core/models/index.ts) pour exporter tous ces nouveaux types.
- [x] Étendre le Service d'API (AC-2)
  - [x] Ouvrir [dashboard-api.service.ts](file:///frontend/src/app/core/services/dashboard-api.service.ts)
  - [x] Importer `AssetDto` et `QuarterlyAlertsResponse` depuis les modèles.
  - [x] Implémenter `getAsset(assetId: string): Observable<AssetDto | null>` appelant le path `/api/dashboard/asset/${assetId}`.
  - [x] Implémenter `getQuarterlyAlerts(): Observable<QuarterlyAlertsResponse | null>` appelant le path `/api/dashboard/alerts/quarterly`.
  - [x] S'assurer que les deux méthodes interceptent les erreurs via `catchError`, affichent le toast en français (« Erreur de connexion avec le serveur, veuillez réessayer plus tard ») via `ToastService` et retournent `of(null)`.
- [x] Écrire les Tests Vitest (AC-3)
  - [x] Créer le fichier `frontend/src/app/core/services/dashboard-api.service.spec.ts`
  - [x] Configurer `TestBed` avec `provideHttpClient()` et `provideHttpClientTesting()`.
  - [x] Injecter et mocker `ToastService` (espionner `showError`).
  - [x] Écrire les tests couvrant les cas nominaux et d'erreurs HTTP.
  - [x] Exécuter `npm test` dans le dossier `frontend` et s'assurer que tous les tests passent.

### Review Findings

- [x] [Review][Patch] Use of forbidden `any` type in `FundamentalsBlock` interface [frontend/src/app/core/models/asset.dto.ts:34]
- [x] [Review][Patch] Missing validation and sanitization for `assetId` parameter [frontend/src/app/core/services/dashboard-api.service.ts:27]
- [x] [Review][Patch] Silent assertion risk in Vitest observable tests [frontend/src/app/core/services/dashboard-api.service.spec.ts:47]
- [x] [Review][Patch] JSDoc documentation for `lastRefresh` field in `AssetDto` [frontend/src/app/core/models/asset.dto.ts:49]
- [x] [Review][Defer] Partial DTO mocking in tests using `as AssetDto` [frontend/src/app/core/services/dashboard-api.service.spec.ts:40] — deferred, pre-existing
- [x] [Review][Defer] Non-standard filename for `toastService.ts` [frontend/src/app/core/services/toastService.ts:1] — deferred, pre-existing

## Dev Notes

### Architecture Patterns et Contraintes

- **Typage strict TypeScript** : Pas de `any` sauf cas exceptionnels de mapping générique. Préférer le typage précis des DTOs.
- **Séparation des dossiers** : Conformément à `project-context.md`, les services résident dans `core/services/` et les types DTO dans `core/models/`.
- **Règle @alex / @steve** : Alex s'occupe de la logique TS front-end. Veiller à n'apporter aucune modification au répertoire `backend/**` dans cette story.
- **Gestion des erreurs** : Le front ne doit jamais propager une exception HTTP vers les composants d'affichage. Il affiche un toast utilisateur en français via le service dédié et retourne `of(null)`.

### References

- [Contexte Projet](file:///c:/Users/chju/Desktop/Dashboard/docs/project-context.md)
- [Endpoint d'Actif Générique (Story 1.5)](file:///_bmad-output/implementation-artifacts/stories/1-5-endpoint-get-api-dashboard-assetid.md)
- [Endpoint d'Alertes Trimestrielles (Story 3.4)](file:///_bmad-output/implementation-artifacts/stories/3-4-alerte-fondamentaux-stale-et-endpoint-quarterly.md)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium)

### Debug Log References

- Task ID: `d3248031-7872-436d-9038-31a46ae215e8/task-110` (npx ng test --watch=false pass)

### Completion Notes List

- Création du fichier `frontend/src/app/core/models/asset.dto.ts` contenant les types strictes TypeScript (`AssetType`, `PriceSource`, `MarketStatus`, `DividendHistoryEntry`, `DividendsBlock`, `HoldingEntry`, `SectorWeight`, `FundamentalsBlock`, `AssetDto`, `StaleAssetAlert`, `QuarterlyAlertsResponse`) qui constituent le miroir complet du backend.
- Mise à jour de `frontend/src/app/core/models/index.ts` pour exporter tous les nouveaux types/interfaces d'actifs.
- Extension de `DashboardApiService` avec les méthodes `getAsset(assetId: string): Observable<AssetDto | null>` et `getQuarterlyAlerts(): Observable<QuarterlyAlertsResponse | null>` avec interception des erreurs via `catchError`, affichage du toast utilisateur en français (« Erreur de connexion avec le serveur, veuillez réessayer plus tard ») via `ToastService` et retour de `of(null)`.
- Création de tests unitaires complets pour `DashboardApiService` sous `frontend/src/app/core/services/dashboard-api.service.spec.ts` à l'aide de `HttpTestingController` pour vérifier les cas nominaux et les cas d'erreur HTTP.
- Exécution et réussite de la suite complète des tests vitest (28 tests passés avec succès).

### File List

- `frontend/src/app/core/models/asset.dto.ts`
- `frontend/src/app/core/models/index.ts`
- `frontend/src/app/core/services/dashboard-api.service.ts`
- `frontend/src/app/core/services/dashboard-api.service.spec.ts`

## Change Log

- 2026-06-30: Implémentation des modèles TypeScript d'actifs et d'alertes trimestrielles, extension du client API et ajout des tests unitaires (Dokk)
