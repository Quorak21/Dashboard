---
story_id: 6.2
story_key: 6-2-realty-income-o
epic: 6
epic_name: Portefeuille élargi — actions, ETF FMP et overview complet
status: done
baseline_commit: 35ff7e0
created: 2026-07-01
FRs:
  - FR-9
  - FR-15
  - FR-16
dependencies:
  - 1.1 (Registre d'actifs YAML et AssetRegistry)
  - 2.2 (AssetSyncJob — tier FMP registry-driven)
  - 3.1 (Configuration dividendes YAML)
  - 3.2 (Configuration fondamentaux YAML)
  - 3.3 (Calcul estimatedYield backend)
  - 4.1 (Modèle AssetDto front et DashboardApiService)
  - 4.3 (Template AssetPage et PriceFreshnessBadge)
  - 6.1 (BlackRock World Mining (brwm))
---

# Story 6.2: Realty Income (o)

Status: review

## Story

En tant que Dokk,
je veux suivre Realty Income en USD avec focus dividende mensuel,
afin de voir mon rendement REIT calculé sur prix live.

## Acceptance Criteria

### AC-1 : Enregistrement de Realty Income dans le Registre
- **Given** le fichier `assets-registry.yml`.
- **When** l'application démarre.
- **Then** l'entrée `o` est enregistrée avec le provider `fmp`, le symbole `O`, le db-symbol `O`, le type `REIT`, la devise `USD`, les heures de marché `America/New_York` (09:30 à 16:00) et la synchro FMP toutes les 15 min.
- **And** les configurations de dividendes et fondamentaux pour `o` (déjà présentes dans les dossiers respectifs) sont chargées au démarrage.

### AC-2 : Récupération du Prix via FMP (USD)
- **Given** le client FMP et le DTO `FMPDto`.
- **When** `FmpPriceProvider.fetch(asset)` récupère le cours pour `O`.
- **Then** le prix retourné est en `USD` (sans division par 100, car la devise brute retournée par l'API FMP est `"USD"`).
- **And** le calcul du rendement (`estimatedYield = forwardDividend / currentPrice * 100`) s'effectue avec ce prix courant en USD (ex: `3.168 / 55.0 * 100 = 5.76%`).
- **And** `FmpPriceProvider` valide que la devise retournée par l'API correspond bien à `USD`.

### AC-3 : Intégration dans la Page de Détails du Frontend
- **Given** la route `/o` dans le frontend Angular.
- **When** Dokk accède à la page.
- **Then** elle charge en lazy-loading le composant `O` qui délègue à `AssetPage` avec `assetId='o'`.
- **And** le badge de fraîcheur de prix (`PriceFreshnessBadge`), les graphiques (annuel et intraday) et les cartes (dividendes et fondamentaux) s'affichent de façon responsive (mobile-first).
- **And** la carte des dividendes met en avant la fréquence mensuelle (`monthly`), affiche le rendement estimé calculé par le backend (`estimatedYield`) et l'historique en `USD` ($).
- **And** la carte des fondamentaux affiche le P/E Ratio (trailing et forward), le debt leverage (35.8%) et le payout ratio (76%) spécifiés dans `fundamentals/o.yml`.

### AC-4 : Intégration sur le Dashboard Principal
- **Given** la page d'accueil (`/`).
- **When** Dokk charge l'accueil.
- **Then** une carte overview pour Realty Income s'affiche sous la section « Stocks ».
- **And** elle affiche le prix courant en USD ($), la variation de 24h, et redirige vers `/o`.

### AC-5 : Tests Automatisés
- **Given** les modifications backend et frontend.
- **When** la suite de tests est exécutée.
- **Then** `mvn test` passe au vert, incluant des tests unitaires pour `FmpPriceProvider` avec une fixture WireMock `profile-o.json` (où le prix est en USD et validé).
- **And** `ng test` (Vitest) passe au vert avec une couverture du composant `O` et de la route.

## Tasks / Subtasks

- [x] **Configuration & Données Backend (AC-1, AC-5)**
  - [x] Ajouter l'entrée `o` dans `backend/src/main/resources/config/assets-registry.yml` selon les spécifications.
  - [x] Ajouter l'entrée `o` dans `backend/src/test/resources/config/assets-registry.yml`.
  - [x] Créer la fixture FMP de test `backend/src/test/resources/fixtures/fmp/profile-o.json` avec un prix simulé de `55.0` et `currency` égal à `"USD"`.
- [x] **Logique & Tests Backend (AC-2, AC-5)**
  - [x] Ajouter un cas de test dans `FmpPriceProviderTest.java` pour valider la récupération du prix USD pour `o` avec sa fixture.
- [x] **Composant et Routage Frontend (AC-3, AC-5)**
  - [x] Créer le composant `O` dans `frontend/src/app/features/stocks/o/` :
    - [x] `o.ts` : charge `AssetPage` avec `assetId='o'`.
    - [x] `o.html` : intègre `<app-asset-page [assetId]="'o'"></app-asset-page>`.
    - [x] [o.spec.ts](file:///frontend/src/app/features/stocks/o/o.spec.ts) : valide le rendu du composant.
  - [x] Déclarer la route `/o` en lazy loading dans `frontend/src/app/app.routes.ts`.
- [x] **Dashboard Overview (AC-4, AC-5)**
  - [x] Ajouter le support du signal et computed pour `o` dans `dashboard.ts` (abonnement à `getAsset('o')`).
  - [x] Ajouter la carte `<app-asset-dashboard-card>` dans `dashboard.html` sous la carte `brwm` avec le symbole de devise `$`.
  - [x] Mettre à jour `dashboard.spec.ts` pour mocker l'appel de `getAsset('o')` et valider les assertions.

### Review Findings

- [x] [Review][Patch] Corrupted British Pound Currency Symbol [frontend/src/app/features/dashboard/dashboard.html:466]
- [x] [Review][Patch] Unhandled RxJS subscription error for hype in dashboard.ts [frontend/src/app/features/dashboard/dashboard.ts:633]
- [x] [Review][Patch] Inefficient WireMock server lifecycle in FmpPriceProviderTest.java [backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java]
- [x] [Review][Patch] Duplicate test setup for AssetDefinition in FmpPriceProviderTest.java [backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java]
- [x] [Review][Patch] Excessive unit test timeout in o.spec.ts [frontend/src/app/features/stocks/o/o.spec.ts:64]
- [x] [Review][Defer] Duplicated assets-registry configuration [backend/src/main/resources/config/assets-registry.yml] — deferred, pre-existing
- [x] [Review][Defer] Severe copy-paste anti-pattern in frontend components [frontend/src/app/features/stocks/] — deferred, pre-existing
- [x] [Review][Defer] Hardcoded route definitions [frontend/src/app/app.routes.ts] — deferred, pre-existing
- [x] [Review][Defer] Inconsistent data types in YAML files [backend/src/main/resources/config/fundamentals/o.yml] — deferred, pre-existing
- [x] [Review][Defer] Repetitive subscription boilerplate in dashboard.ts [frontend/src/app/features/dashboard/dashboard.ts] — deferred, pre-existing

## Dev Notes

### Architecture Patterns et Contraintes
- **Standalone Angular 21** : Tous les nouveaux composants doivent être Standalone (pas de `NgModule`).
- **RxJS Memory Leak Prevention** : Ne PAS utiliser `takeUntilDestroyed(this.destroyRef)` à l'intérieur de méthodes répétitives comme `refresh()`. Gérer proprement le cycle de vie des abonnements en effectuant un `unsubscribe()` manuel dans le crochet de destruction de `DestroyRef`.
- **Conversion d'échelle & Devises** : Ne pas coder en dur de multiplicateurs dans les composants d'affichage. La devise `"USD"` ne doit pas être normalisée ou modifiée par le provider de prix FMP.
- **Thème Copper & Dark** : Utiliser la palette de styles du projet (`bg-dark-900`, `text-copper-400`, etc.) et s'assurer de la réactivité mobile-first.

### Source tree components to touch
- `backend/src/main/resources/config/assets-registry.yml` (UPDATE)
- `backend/src/test/resources/config/assets-registry.yml` (UPDATE)
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java` (UPDATE)
- `frontend/src/app/app.routes.ts` (UPDATE)
- `frontend/src/app/features/dashboard/dashboard.ts` (UPDATE)
- `frontend/src/app/features/dashboard/dashboard.html` (UPDATE)
- `frontend/src/app/features/dashboard/dashboard.spec.ts` (UPDATE)
- `frontend/src/app/features/stocks/o/` (NEW)
- `backend/src/test/resources/fixtures/fmp/profile-o.json` (NEW)

### References
- Fichier des Epics : [epics.md](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/epics.md#Story%206.2)
- Guide des règles du projet : [project-context.md](file:///c:/Users/chju/Desktop/Dashboard/docs/project-context.md)

## Dev Agent Record

### Agent Model Used
Gemini 3.5 Flash (Medium)

### Debug Log References
- `.\mvnw.cmd test` : 109 tests run successfully.
- `npm test` : 43 tests run successfully.

### Completion Notes List
- Added entry `o` to `assets-registry.yml` for main and test environments.
- Created test profile `profile-o.json` in USD.
- Added USD fetching test `fetch_retrievesPriceInUsdForNyseStocks` to `FmpPriceProviderTest.java`.
- Created standalone component `O` and declared lazy route `/o`.
- Added signals, computed properties, unsubscription, and refresh logic for `o` in `dashboard.ts`.
- Added card for `o` in `dashboard.html` and mock/assertions in `dashboard.spec.ts`.

### File List
- `backend/src/main/resources/config/assets-registry.yml`
- `backend/src/test/resources/config/assets-registry.yml`
- `backend/src/test/resources/fixtures/fmp/profile-o.json`
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/features/stocks/o/o.ts`
- `frontend/src/app/features/stocks/o/o.html`
- `frontend/src/app/features/stocks/o/o.spec.ts`
- `frontend/src/app/features/dashboard/dashboard.ts`
- `frontend/src/app/features/dashboard/dashboard.html`
- `frontend/src/app/features/dashboard/dashboard.spec.ts`
