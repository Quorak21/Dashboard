---
story_id: 6.4
story_key: 6-4-ishares-global-infrastructure-infr-et-etfsectorchart
epic: 6
epic_name: Portefeuille élargi — actions, ETF FMP et overview complet
status: done
baseline_commit: 35ff7e0
created: 2026-07-02
FRs:
  - FR-13
  - FR-16
dependencies:
  - 1.1 (Registre d'actifs YAML et AssetRegistry)
  - 2.2 (AssetSyncJob — tier FMP registry-driven)
  - 3.1 (Configuration dividendes YAML)
  - 3.2 (Configuration fondamentaux YAML)
  - 4.1 (Modèle AssetDto front et DashboardApiService)
  - 4.3 (Template AssetPage et PriceFreshnessBadge)
  - 6.1 (BlackRock World Mining — brwm)
  - 6.2 (Realty Income — o)
  - 6.3 (3i Group — iii)
---

# Story 6.4: iShares Global Infrastructure (infr) et EtfSectorChart

Status: done

## Story

En tant que Dokk,
je veux voir INFR avec répartition sectorielle,
afin de comprendre l'exposition infrastructure.

## Acceptance Criteria

### AC-1 : Enregistrement de infr dans le Registre
- **Given** le fichier `backend/src/main/resources/config/assets-registry.yml`.
- **When** l'application démarre.
- **Then** l'entrée `infr` est enregistrée avec :
  - `provider: fmp`
  - `symbol: INFR.L`
  - `db-symbol: INFR`
  - `type: ETF`
  - `currency: USD`
  - `market-hours`: `zone: Europe/London`, `open: "08:00"`, `close: "16:35"`
  - `sync.interval-minutes: 15`, `sync.offset-minutes: 9` (décalage unique pour éviter les conflits scheduler avec brwm et iii)
- **And** les configs dividendes (`dividends/infr.yml`) et fondamentaux (`fundamentals/infr.yml`) déjà présentes sont chargées au démarrage sans erreur.

### AC-2 : Récupération du Prix via FMP (USD)
- **Given** le client FMP et l'actif `infr` avec currency `USD`.
- **When** `FmpPriceProvider.fetch(asset)` récupère le cours pour `INFR.L`.
- **Then** si l'API FMP retourne `currency: "USD"`, le prix et la devise sont conservés tels quels sans division ni modification.
- **And** si l'API FMP retourne une autre devise (comme `GBp` ou `GBP`), `FmpPriceProvider` doit lever une `ExternalProviderException` (la devise attendue configurée étant USD).
- **And** un test WireMock `fetch_retrievesPriceInUsdForInfr` avec fixture `profile-infr.json` (prix en USD, ex: 24.50) valide le fetch.

### AC-3 : Création du composant EtfMetricsCard et intégration
- **Given** les données fondamentales de l'actif `infr`.
- **When** le composant `EtfMetricsCard` (`app-etf-metrics-card`) est rendu.
- **Then** il affiche de manière responsive (mobile-first) les métriques ETF suivantes extraites de la configuration de fondamentaux (`fundamentals/infr.yml`) :
  - **TER** (Management Fee) : valeur brute (ex: `0.65%`)
  - **AUM** (Total Assets) : valeur brute (ex: `$2.8bn`)
  - **NAV Premium/Discount** (nav-discount-premium) : valeur brute (ex: `0.02%`)
- **And** il affiche la liste des **Top Holdings** extraite de la configuration de fondamentaux (nom de l'action et poids en pourcentage).

### AC-4 : Création du composant EtfSectorChart et intégration
- **Given** les données fondamentales de l'actif `infr`.
- **When** le composant `EtfSectorChart` (`app-etf-sector-chart`) est rendu.
- **Then** il affiche un graphique de type Donut (ou Pie) en utilisant **Chart.js** représentant la répartition sectorielle de l'ETF (sector-weights de `fundamentals/infr.yml`).
- **And** le thème du graphique s'intègre harmonieusement avec la palette de couleurs existante (dark & copper).

### AC-5 : Intégration sur la Page de Détails et Routage
- **Given** la route `/infr` dans le frontend Angular.
- **When** Dokk accède à la page.
- **Then** elle charge en lazy-loading le composant `Infr` (classe) depuis `features/stocks/infr/infr.ts` (ou `features/etfs/infr/` si un dossier générique est souhaité, mais `features/stocks/infr/` est préféré pour cohérence avec les autres stocks) qui délègue à `<app-asset-page [assetId]="'infr'">`.
- **And** la page de détails affiche la carte principale, les graphiques (annuel et intraday) et remplace la `FundamentalsCard` classique par `EtfMetricsCard` et `EtfSectorChart` si l'actif est de type `ETF`.
- **And** le badge de fraîcheur de prix affiche les bonnes informations en USD.

### AC-6 : Tests Automatisés
- **Given** les modifications backend et frontend.
- **When** la suite de tests est exécutée.
- **Then** `mvn test` passe au vert (111+ tests), incluant le nouveau test WireMock pour `infr` dans `FmpPriceProviderTest.java`.
- **And** `ng test` (Vitest) passe au vert (47+ tests), incluant des tests unitaires pour `EtfMetricsCard`, `EtfSectorChart` et le composant `Infr`.

## Tasks / Subtasks

- [x] **Configuration Backend (AC-1)**
  - [x] Ajouter l'entrée `infr` dans `backend/src/main/resources/config/assets-registry.yml` (après l'entrée `iii`).
  - [x] Ajouter l'entrée `infr` dans `backend/src/test/resources/config/assets-registry.yml` (identique).

- [x] **Test Backend — Fixture et WireMock (AC-2, AC-6)**
  - [x] Créer la fixture `backend/src/test/resources/fixtures/fmp/profile-infr.json` avec `price: 24.50`, `currency: "USD"`, `marketCap`, `changePercentage`, `volume`.
  - [x] Ajouter le test `fetch_retrievesPriceInUsdForInfr` dans `FmpPriceProviderTest.java` — valider fetch d'un actif en USD et vérification de devise.

- [x] **Composants Partagés Frontend (AC-3, AC-4, AC-6)**
  - [x] Créer `frontend/src/app/shared/components/etf-metrics-card/etf-metrics-card.ts` (standalone, displays TER, AUM, NAV premium/discount and Top Holdings list).
  - [x] Créer `frontend/src/app/shared/components/etf-metrics-card/etf-metrics-card.html` et `.css` associés.
  - [x] Créer `frontend/src/app/shared/components/etf-metrics-card/etf-metrics-card.spec.ts`.
  - [x] Créer `frontend/src/app/shared/components/etf-sector-chart/etf-sector-chart.ts` (standalone, uses Chart.js to render a donut chart representing sector weights).
  - [x] Créer `frontend/src/app/shared/components/etf-sector-chart/etf-sector-chart.html` et `.css` associés.
  - [x] Créer `frontend/src/app/shared/components/etf-sector-chart/etf-sector-chart.spec.ts`.

- [x] **Intégration AssetPage (AC-5)**
  - [x] Modifier `frontend/src/app/shared/components/asset-page/asset-page.ts` : importer `EtfMetricsCard` et `EtfSectorChart`. Ajouter un computed property pour savoir si l'actif est un ETF (ex: `isEtf = computed(() => this.data()?.type === 'ETF')`).
  - [x] Modifier `frontend/src/app/shared/components/asset-page/asset-page.html` : utiliser `@if (isEtf())` pour afficher `app-etf-metrics-card` and `app-etf-sector-chart` à la place de `app-fundamentals-card`.

- [x] **Composant et Routage Frontend infr (AC-5, AC-6)**
  - [x] Créer `frontend/src/app/features/stocks/infr/infr.ts` (standalone wrapping assetId `infr`).
  - [x] Créer `frontend/src/app/features/stocks/infr/infr.html` avec `<app-asset-page [assetId]="'infr'"></app-asset-page>`.
  - [x] Créer `frontend/src/app/features/stocks/infr/infr.spec.ts`.
  - [x] Déclarer la route `/infr` en lazy-loading dans `frontend/src/app/app.routes.ts`.

  ### Review Findings

  - [x] [Review][Patch] API request fails on load terminates stream [frontend/src/app/shared/components/asset-page/asset-page.ts:1005]
  - [x] [Review][Patch] Race condition on background refresh [frontend/src/app/shared/components/asset-page/asset-page.ts:1027-1030]
  - [x] [Review][Patch] Unhandled RxJS error during refresh [frontend/src/app/shared/components/asset-page/asset-page.ts:1027]
  - [x] [Review][Patch] Update chart backgroundColors on weights data change [frontend/src/app/shared/components/etf-sector-chart/etf-sector-chart.ts:1437-1442]
  - [x] [Review][Patch] ETF sector count > 7 transparent segments [frontend/src/app/shared/components/etf-sector-chart/etf-sector-chart.ts:1470]
  - [x] [Review][Patch] Null or undefined entry in topHoldings crashes component [frontend/src/app/shared/components/etf-metrics-card/etf-metrics-card.ts:1269]
  - [x] [Review][Patch] Low contrast of metrics values text in ETF card [frontend/src/app/shared/components/etf-metrics-card/etf-metrics-card.html:1074]
  - [x] [Review][Patch] Responsive legend position for mobile in sector chart [frontend/src/app/shared/components/etf-sector-chart/etf-sector-chart.ts:1481]
  - [x] [Review][Patch] Asset main card type tag stock is hardcoded [frontend/src/app/shared/components/asset-page/asset-page.html:696]
  - [x] [Review][Patch] Esthetic misalignment when dividends present [frontend/src/app/shared/components/asset-page/asset-page.html:732]
  - [x] [Review][Patch] Absence of currency mismatch unit test for infr [backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java]
  - [x] [Review][Defer] Route and wrapper component duplication [frontend/src/app/app.routes.ts] — deferred, pre-existing

## Dev Notes

### Architecture Patterns — Critiques à Respecter

1. **Composant minimal Standalone** : Le composant `Infr` ne contient aucune logique propre, il est une simple enveloppe.
2. **Chart.js Donut integration** : Le composant `EtfSectorChart` doit instancier le graphique Chart.js de type `donut`. Veiller à importer et enregistrer les composants de Chart.js nécessaires (ex: `DoughnutController`, `ArcElement`, `Legend`, `Tooltip`).
3. **Responsive Mobile-first** : Les deux cartes d'ETF doivent s'empiler sur mobile et s'afficher côte à côte sur écran plus large, en phase avec la grille de l'application.

### References
- Spec components pattern : [fundamentals-card.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/shared/components/fundamentals-card/fundamentals-card.ts)
- Test component pattern : [fundamentals-card.spec.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/shared/components/fundamentals-card/fundamentals-card.spec.ts)
- AssetPage : [asset-page.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/shared/components/asset-page/asset-page.ts)
- FmpPriceProviderTest : [FmpPriceProviderTest.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java)
- Épics : [epics.md](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/epics.md)
- PRD : [prd.md](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/prds/prd-Dashboard-2026-06-17/prd.md)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium)

### Debug Log References

- Task-247 (Backend unit tests passed)
- Task-311 (Frontend unit tests passed)

### Completion Notes List

- Added entry `infr` to assets-registry.yml for both production and test.
- Implemented price fetching in USD from FMP profile without currency mismatch errors, verified with mock profile-infr.json.
- Created standalone shared components `EtfMetricsCard` (displays TER, AUM, NAV discount/premium, and Top Holdings list) and `EtfSectorChart` (doughnut chart displaying sector exposure).
- Integrated both components in `AssetPage` to render when the asset type is `ETF`.
- Created feature component `Infr` to render `AssetPage` with assetId `infr`.
- Registered route `/infr` as a lazy-loaded route in Angular routes.
- Wrote unit tests for all new components and verified all tests pass.

### File List

- `backend/src/main/resources/config/assets-registry.yml`
- `backend/src/test/resources/config/assets-registry.yml`
- `backend/src/test/resources/fixtures/fmp/profile-infr.json`
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/features/stocks/infr/infr.ts`
- `frontend/src/app/features/stocks/infr/infr.html`
- `frontend/src/app/features/stocks/infr/infr.spec.ts`
- `frontend/src/app/shared/components/asset-page/asset-page.ts`
- `frontend/src/app/shared/components/asset-page/asset-page.html`
- `frontend/src/app/shared/components/asset-page/asset-page.spec.ts`
- `frontend/src/app/shared/components/etf-metrics-card/etf-metrics-card.ts`
- `frontend/src/app/shared/components/etf-metrics-card/etf-metrics-card.html`
- `frontend/src/app/shared/components/etf-metrics-card/etf-metrics-card.css`
- `frontend/src/app/shared/components/etf-metrics-card/etf-metrics-card.spec.ts`
- `frontend/src/app/shared/components/etf-sector-chart/etf-sector-chart.ts`
- `frontend/src/app/shared/components/etf-sector-chart/etf-sector-chart.html`
- `frontend/src/app/shared/components/etf-sector-chart/etf-sector-chart.css`
- `frontend/src/app/shared/components/etf-sector-chart/etf-sector-chart.spec.ts`
