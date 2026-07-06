---
story_id: 6.1
story_key: 6-1-blackrock-world-mining-brwm
epic: 6
epic_name: Portefeuille élargi — actions, ETF FMP et overview complet
status: done
baseline_commit: 35ff7e0
created: 2026-07-01
FRs:
  - FR-8
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
---

# Story 6.1: BlackRock World Mining (brwm)

Status: done

## Story

En tant que Dokk,
je veux suivre BRWM avec NAV/discount et dividendes config,
afin de monitorer mon trust minière LSE.

## Acceptance Criteria

### AC-1 : Enregistrement de BRWM dans le Registre
- **Given** le fichier `assets-registry.yml`.
- **When** l'application démarre.
- **Then** l'entrée `brwm` est enregistrée avec le provider `fmp`, le symbole `BRWM.L`, le db-symbol `BRWM`, le type `TRUST`, la devise `GBP`, les heures de marché `Europe/London` (08:00 à 16:35) et la synchro FMP toutes les 15 min (offset de 3 minutes).
- **And** les configurations de dividendes et fondamentaux pour `brwm` sont chargées au démarrage.

### AC-2 : Normalisation des Prix FMP (GBp vers GBP)
- **Given** le client FMP et le DTO `FMPDto`.
- **When** `FmpPriceProvider.fetch(asset)` récupère le cours pour un symbole coté sur la LSE (ex: `BRWM.L`) dont le champ `currency` brut de l'API FMP est `"GBp"` (pence).
- **Then** le prix retourné est normalisé en `GBP` (livres sterling) en le divisant par 100 (ex: `1027.6` GBp devient `10.276` GBP).
- **And** le calcul du rendement (`estimatedYield = forwardDividend / currentPrice * 100`) s'effectue sur ce prix normalisé (ex: `0.185 / 10.276 * 100 = 1.80 %` au lieu de `0.018 %`).
- **And** les points de cours quotidiens dans `AssetDaily` sous le symbole `BRWM` sont également persistés avec le prix normalisé.

### AC-3 : Intégration dans la Page de Détails du Frontend
- **Given** la route `/brwm` dans le frontend Angular.
- **When** Dokk accède à la page.
- **Then** elle charge en lazy-loading le composant `Brwm` qui délègue à `AssetPage` avec `assetId='brwm'`.
- **And** le badge de fraîcheur de prix (`PriceFreshnessBadge`), les graphiques (annuel et intraday) et les cartes (dividendes et fondamentaux) s'affichent de façon responsive (mobile-first).
- **And** la carte des dividendes affiche le rendement estimé calculé par le backend (`estimatedYield`) et l'historique en `GBP` (£).
- **And** la carte des fondamentaux affiche la valeur nette d'actifs (NAV), la décote/prime (`nav-discount-premium`), ainsi que les top holdings et sector weights spécifiés dans `fundamentals/brwm.yml`.

### AC-4 : Intégration sur le Dashboard Principal
- **Given** la page d'accueil (`/`).
- **When** Dokk charge l'accueil.
- **Then** une carte overview pour BRWM s'affiche sous la section « Stocks » (ou une section dédiée).
- **And** elle affiche le prix courant normalisé en GBP (£), la variation de 24h, et redirige vers `/brwm`.

### AC-5 : Tests Automatisés
- **Given** les modifications backend et frontend.
- **When** la suite de tests est exécutée.
- **Then** `mvn test` passe au vert, incluant des tests unitaires pour `FmpPriceProvider` avec une fixture WireMock `profile-brwm.json` (où le prix est en GBp et validé après normalisation).
- **And** `ng test` (Vitest) passe au vert avec une couverture des composants `Brwm` et de la route.

## Tasks / Subtasks

- [x] **Configuration & Données Backend (AC-1, AC-5)**
  - [x] Ajouter l'entrée `brwm` dans [assets-registry.yml](file:///backend/src/main/resources/config/assets-registry.yml) selon les spécifications.
  - [x] Créer la fixture FMP de test [profile-brwm.json](file:///backend/src/test/resources/fixtures/fmp/profile-brwm.json) avec un prix simulé de `550.0` et `currency` égal à `"GBp"`.
- [x] **Logique de Normalisation (AC-2, AC-5)**
  - [x] Modifier [FMPDto.java](file:///backend/src/main/java/com/dokkcorp/dashboard/providers/fmp/FMPDto.java) pour mapper le champ `currency` de la réponse API.
  - [x] Adapter [FmpPriceProvider.java](file:///backend/src/main/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProvider.java) pour détecter la devise `"GBp"` et normaliser le prix récupéré (division par 100) avant de créer le `PriceQuote`.
  - [x] Ajouter un cas de test dans [FmpPriceProviderTest.java](file:///backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java) pour valider cette conversion d'échelle.
- [x] **Composant et Routage Frontend (AC-3, AC-5)**
  - [x] Créer le composant `Brwm` dans `frontend/src/app/features/stocks/brwm/` :
    - [x] [brwm.ts](file:///frontend/src/app/features/stocks/brwm/brwm.ts) : charge `AssetPage` avec `assetId='brwm'`.
    - [x] [brwm.html](file:///frontend/src/app/features/stocks/brwm/brwm.html) : intègre `<app-asset-page [assetId]="'brwm'"></app-asset-page>`.
    - [x] [brwm.spec.ts](file:///frontend/src/app/features/stocks/brwm/brwm.spec.ts) : valide le rendu du composant.
  - [x] Déclarer la route `/brwm` en lazy loading dans [app.routes.ts](file:///frontend/src/app/app.routes.ts).
- [x] **Dashboard Overview (AC-4, AC-5)**
  - [x] Ajouter le support du signal et computed pour `brwm` dans [dashboard.ts](file:///frontend/src/app/features/dashboard/dashboard.ts) (abonnement à `getAsset('brwm')` et nettoyage RxJS via `takeUntilDestroyed`).
  - [x] Ajouter la carte `<app-asset-dashboard-card>` dans [dashboard.html](file:///frontend/src/app/features/dashboard/dashboard.html) sous la carte `inveb` avec le symbole de devise `£`.
  - [x] Mettre à jour [dashboard.spec.ts](file:///frontend/src/app/features/dashboard/dashboard.spec.ts) pour mocker l'appel de `getAsset('brwm')` et valider les assertions.

### Review Findings

- [x] [Review][Patch] Incorrect Currency Case-Insensitivity Check [backend/src/main/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProvider.java:38]
- [x] [Review][Patch] Angular DestroyRef Memory Leak in Dashboard [frontend/src/app/features/dashboard/dashboard.ts:51]
- [x] [Review][Patch] Lack of Currency Validation in FmpPriceProvider [backend/src/main/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProvider.java:41]
- [x] [Review][Patch] Redundant Empty Check in FmpPriceProvider [backend/src/main/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProvider.java:33]
- [x] [Review][Defer] Highly Hardcoded Asset Rendering in Dashboard [frontend/src/app/features/dashboard/dashboard.html:42] — deferred, pre-existing
- [x] [Review][Defer] Hardcoded UI Currency Symbols in Template [frontend/src/app/features/dashboard/dashboard.html:44] — deferred, pre-existing
- [x] [Review][Defer] Mixed French/English in Unit Tests [frontend/src/app/features/dashboard/dashboard.spec.ts:1] — deferred, pre-existing
- [x] [Review][Defer] Silent Coercion of Missing Numeric Values to Zero [backend/src/main/java/com/dokkcorp/dashboard/providers/fmp/FMPDto.java:7] — deferred, pre-existing

## Dev Notes

### Architecture Patterns et Contraintes
- **Standalone Angular 21** : Tous les nouveaux composants doivent être Standalone (pas de `NgModule`).
- **RxJS Leak Prevention** : S'abonner avec `.pipe(takeUntilDestroyed(this.destroyRef))` pour détruire automatiquement les abonnements.
- **Conversion d'échelle** : Ne pas coder en dur des multiplicateurs dans les composants d'affichage. La conversion GBp -> GBP doit être gérée par le backend au niveau du provider de prix FMP.
- **Thème Copper & Dark** : Utiliser la palette de styles du projet (`bg-dark-900`, `text-copper-400`, etc.) et s'assurer de la réactivité mobile-first.

### Source tree components to touch
- [assets-registry.yml](file:///backend/src/main/resources/config/assets-registry.yml) (UPDATE)
- [FMPDto.java](file:///backend/src/main/java/com/dokkcorp/dashboard/providers/fmp/FMPDto.java) (UPDATE)
- [FmpPriceProvider.java](file:///backend/src/main/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProvider.java) (UPDATE)
- [FmpPriceProviderTest.java](file:///backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java) (UPDATE)
- [app.routes.ts](file:///frontend/src/app/app.routes.ts) (UPDATE)
- [dashboard.ts](file:///frontend/src/app/features/dashboard/dashboard.ts) (UPDATE)
- [dashboard.html](file:///frontend/src/app/features/dashboard/dashboard.html) (UPDATE)
- [dashboard.spec.ts](file:///frontend/src/app/features/dashboard/dashboard.spec.ts) (UPDATE)
- [brwm/](file:///frontend/src/app/features/stocks/brwm/) (NEW)
- [profile-brwm.json](file:///backend/src/test/resources/fixtures/fmp/profile-brwm.json) (NEW)

### References
- Fichier des Epics : [epics.md](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/epics.md#Story%206.1)
- Guide des règles du projet : [project-context.md](file:///c:/Users/chju/Desktop/Dashboard/docs/project-context.md)

## Dev Agent Record

### Agent Model Used
Gemini 3.5 Flash (Medium)

### Debug Log References
- `mvnw.cmd test` : 106 tests exécutés et réussis (BUILD SUCCESS).
- `ng test` (Vitest) : 42 tests exécutés et réussis (15 fichiers de tests).

### Completion Notes List
- Configuration de l'actif `brwm` de type `TRUST` dans `assets-registry.yml` pour le backend et les tests.
- Ajout du champ `currency` dans `FMPDto` et implémentation de la normalisation de la devise `"GBp"` vers `"GBP"` par division par 100 dans `FmpPriceProvider`.
- Ajout de tests unitaires validant la conversion et le calcul de rendement normalisé dans `FmpPriceProviderTest`.
- Déclaration de la route `/brwm` en lazy loading dans `app.routes.ts`.
- Création du composant stocks/brwm (`brwm.ts`, `brwm.html`, `brwm.spec.ts`) pour déléguer à `AssetPage`.
- Intégration de l'actif BRWM dans le dashboard (`dashboard.ts`, `dashboard.html`) et tests unitaires mis à jour dans `dashboard.spec.ts`.

### File List
- `backend/src/main/resources/config/assets-registry.yml`
- `backend/src/test/resources/config/assets-registry.yml`
- `backend/src/main/java/com/dokkcorp/dashboard/providers/fmp/FMPDto.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProvider.java`
- `backend/src/test/resources/fixtures/fmp/profile-brwm.json`
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/features/stocks/brwm/brwm.ts`
- `frontend/src/app/features/stocks/brwm/brwm.html`
- `frontend/src/app/features/stocks/brwm/brwm.spec.ts`
- `frontend/src/app/features/dashboard/dashboard.ts`
- `frontend/src/app/features/dashboard/dashboard.html`
- `frontend/src/app/features/dashboard/dashboard.spec.ts`
