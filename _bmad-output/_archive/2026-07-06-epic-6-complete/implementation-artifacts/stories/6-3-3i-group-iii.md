---
story_id: 6.3
story_key: 6-3-3i-group-iii
epic: 6
epic_name: Portefeuille élargi — actions, ETF FMP et overview complet
status: done
baseline_commit: 35ff7e0
created: 2026-07-01
FRs:
  - FR-10
  - FR-16
dependencies:
  - 1.1 (Registre d'actifs YAML et AssetRegistry)
  - 2.2 (AssetSyncJob — tier FMP registry-driven)
  - 3.1 (Configuration dividendes YAML)
  - 3.2 (Configuration fondamentaux YAML)
  - 3.3 (Calcul estimatedYield backend)
  - 4.1 (Modèle AssetDto front et DashboardApiService)
  - 4.3 (Template AssetPage et PriceFreshnessBadge)
  - 6.1 (BlackRock World Mining — brwm), 6.2 (Realty Income — o)
---

# Story 6.3: 3i Group (iii)

Status: done

## Story

En tant que Dokk,
je veux suivre 3i Group avec valorisation NAV/discount,
afin d'évaluer la décote du trust private equity sur le prix de marché live.

## Acceptance Criteria

### AC-1 : Enregistrement de 3i Group dans le Registre
- **Given** le fichier `backend/src/main/resources/config/assets-registry.yml`.
- **When** l'application démarre.
- **Then** l'entrée `iii` est enregistrée avec :
  - `provider: fmp`
  - `symbol: III.L`
  - `db-symbol: III`
  - `type: TRUST`
  - `currency: GBP`
  - `market-hours`: `zone: Europe/London`, `open: "08:00"`, `close: "16:35"`
  - `sync.interval-minutes: 15`, `sync.offset-minutes: 6` (décalage unique pour éviter simultanéité avec brwm)
- **And** les configs dividendes (`dividends/iii.yml`) et fondamentaux (`fundamentals/iii.yml`) déjà présentes sont chargées au démarrage sans erreur.

### AC-2 : Récupération et normalisation du Prix via FMP (GBp → GBP)
- **Given** le client FMP et l'actif `iii` avec currency `GBP`.
- **When** `FmpPriceProvider.fetch(asset)` récupère le cours pour `III.L`.
- **Then** si l'API FMP retourne `currency: "GBp"` (pence), `FmpPriceProvider` divise le prix par 100 et normalise la devise à `"GBP"` — exactement comme pour `brwm`.
- **And** si l'API FMP retourne `currency: "GBP"` (déjà en livres), le prix est retourné sans division.
- **And** le calcul `estimatedYield = forwardDividend / currentPrice * 100` s'effectue avec le prix normalisé en GBP.
- **And** un test WireMock `fetch_convertsGBpToGbpFor3iGroup` avec fixture `profile-iii.json` (prix en pence, ex: 3180 GBp → 31.80 GBP) valide la normalisation.

### AC-3 : Intégration dans la Page de Détails du Frontend
- **Given** la route `/iii` dans le frontend Angular.
- **When** Dokk accède à la page.
- **Then** elle charge en lazy-loading le composant `Iii` (classe) depuis `features/stocks/iii/iii.ts` qui délègue à `<app-asset-page [assetId]="'iii'">`.
- **And** le badge de fraîcheur de prix, les graphiques (annuel et intraday) et les cartes (dividendes et fondamentaux) s'affichent de façon responsive (mobile-first).
- **And** la `FundamentalsCard` affiche les métriques NAV/discount depuis les données de `fundamentals/iii.yml` (trailing-pe, forward-pe, debt-leverage, dividend-payout-ratio).
- **And** la `DividendCard` affiche le rendement estimé calculé par le backend et l'historique en GBP.

### AC-4 : Intégration sur le Dashboard Principal
- **Given** la page d'accueil (`/`).
- **When** Dokk charge l'accueil.
- **Then** une carte overview pour 3i Group s'affiche sous la section « Stocks » après la carte Realty Income.
- **And** elle affiche le prix courant en GBP (£), la variation de 24h, et redirige vers `/iii`.

### AC-5 : Tests Automatisés
- **Given** les modifications backend et frontend.
- **When** la suite de tests est exécutée.
- **Then** `mvn test` passe au vert (109+ tests), incluant le nouveau test WireMock pour `iii` dans `FmpPriceProviderTest.java`.
- **And** `ng test` (Vitest) passe au vert (43+ tests), incluant un test pour le composant `Iii` dans `iii.spec.ts` et les assertions mises à jour dans `dashboard.spec.ts`.

## Tasks / Subtasks

- [x] **Configuration Backend (AC-1)**
  - [x] Ajouter l'entrée `iii` dans `backend/src/main/resources/config/assets-registry.yml` (après l'entrée `o`).
  - [x] Ajouter l'entrée `iii` dans `backend/src/test/resources/config/assets-registry.yml` (identique).

- [x] **Test Backend — Fixture et WireMock (AC-2, AC-5)**
  - [x] Créer la fixture `backend/src/test/resources/fixtures/fmp/profile-iii.json` avec `price: 3180.0`, `currency: "GBp"` (pence), `marketCap`, `changePercentage`, `volume`.
  - [x] Ajouter le test `fetch_convertsGBpToGbpFor3iGroup` dans `FmpPriceProviderTest.java` — valider normalisation pence→livres et currency GBP.

- [x] **Composant et Routage Frontend (AC-3, AC-5)**
  - [x] Créer `frontend/src/app/features/stocks/iii/iii.ts` (standalone, imports: AssetPage, templateUrl: `./iii.html`).
  - [x] Créer `frontend/src/app/features/stocks/iii/iii.html` avec `<app-asset-page [assetId]="'iii'"></app-asset-page>`.
  - [x] Créer `frontend/src/app/features/stocks/iii/iii.spec.ts` (pattern brwm.spec.ts, mock AssetDto avec assetId: 'iii', currency: 'GBP', type: 'TRUST').
  - [x] Déclarer la route `/iii` en lazy-loading dans `frontend/src/app/app.routes.ts`.

- [x] **Dashboard Overview (AC-4, AC-5)**
  - [x] Ajouter `private iiiAsset = signal<AssetDto | null>(null)` et `private iiiSub?: Subscription` dans `dashboard.ts`.
  - [x] Ajouter `iiiPrice = computed(...)` et `iiiChange = computed(...)` dans `dashboard.ts`.
  - [x] Ajouter `this.iiiSub?.unsubscribe()` dans le bloc `destroyRef.onDestroy()`.
  - [x] Ajouter le bloc subscription `iiiSub` dans `refresh()` avec next/error handler.
  - [x] Ajouter la carte `<app-asset-dashboard-card asset="iii" title="3i Group" ...>` dans `dashboard.html` après la carte `o` avec `currencySymbol="£"`.
  - [x] Mettre à jour `dashboard.spec.ts` : ajouter mock `mockIiiDto`, câbler `getAsset` pour `'iii'`, ajouter assertions pour `iiiPrice()` et `iiiChange()`, vérifier appel `getAsset('iii')`.

### Review Findings

- [x] [Review][Patch] Incomplete and Language-Mixed Test Descriptions in dashboard.spec.ts [frontend/src/app/features/dashboard/dashboard.spec.ts:80,105]
- [x] [Review][Patch] Superfluous Trailing Empty Lines in Registry Configurations [backend/src/main/resources/config/assets-registry.yml:63-66]
- [x] [Review][Defer] Duplicate Registry Configuration Files [backend/src/main/resources/config/assets-registry.yml] — deferred, pre-existing
- [x] [Review][Defer] Configuration Mismatch in Test Data for brwm [backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java:137] — deferred, pre-existing
- [x] [Review][Defer] Severe Angular Component and Route Duplication [frontend/src/app/app.routes.ts] — deferred, pre-existing
- [x] [Review][Defer] Repetitive Manual Subscription Management on Dashboard [frontend/src/app/features/dashboard/dashboard.ts] — deferred, pre-existing
- [x] [Review][Defer] Hardcoded UI Labels and Currency Symbols on Dashboard [frontend/src/app/features/dashboard/dashboard.html] — deferred, pre-existing
- [x] [Review][Defer] Redundant Global Mocking of ResizeObserver [frontend/src/app/features/stocks/iii/iii.spec.ts] — deferred, pre-existing
- [x] [Review][Defer] Inconsistent Test Fixture Retrieval Strategy [backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java] — deferred, pre-existing
- [x] [Review][Defer] Inconsistent Data Scale in Mock Objects [frontend/src/app/features/stocks/iii/iii.spec.ts] — deferred, pre-existing

## Dev Notes

### Architecture Patterns — Critiques à Respecter

**Pattern établi par 6.1 (brwm) et 6.2 (o) — NE PAS dévier :**

1. **Composant minimal Standalone** : Le composant ne contient aucune logique propre. Il est une simple enveloppe :
   ```typescript
   @Component({ selector: 'app-iii', imports: [AssetPage], templateUrl: './iii.html' })
   export class Iii {}
   ```

2. **HTML minimaliste** :
   ```html
   <app-asset-page [assetId]="'iii'"></app-asset-page>
   ```

3. **RxJS Memory Leak** : Ne PAS utiliser `takeUntilDestroyed(this.destroyRef)` dans `refresh()`. Utiliser le pattern `iiiSub?.unsubscribe()` dans `refresh()` + `destroyRef.onDestroy()`. Voir `dashboard.ts` lignes 47–53 et 56–88 pour le pattern exact.

4. **Gestion d'erreur** : Chaque subscription doit avoir un bloc `{ next: ..., error: () => this.iiiAsset.set(null) }`.

5. **Route lazy** : Ajouter dans `app.routes.ts` avant le wildcard `**`:
   ```typescript
   { path: 'iii', loadComponent: () => import('./features/stocks/iii/iii').then(m => m.Iii) }
   ```

### Normalisation GBp → GBP — Critique pour iii

Le price provider FMP retourne souvent les actifs LSE en **pence (GBp)** plutôt qu'en **livres sterling (GBP)**. La logique de normalisation est déjà implémentée dans `FmpPriceProvider.java` et a été validée pour `brwm`.

- Si `currency == "GBp"` (pence) : `price / 100`, `currency = "GBP"`
- Si `currency == "GBP"` (livres) : aucune transformation

**3i Group (III.L) sur le LSE** : L'API FMP retourne typiquement le prix en pence. Ex: 3180 GBp = 31.80 GBP. La fixture de test doit refléter ce cas.

### Source tree — Fichiers à toucher

| Fichier | Action | Notes |
|---------|--------|-------|
| `backend/src/main/resources/config/assets-registry.yml` | UPDATE | Ajouter entrée `iii` après `o` |
| `backend/src/test/resources/config/assets-registry.yml` | UPDATE | Même entrée que main |
| `backend/src/test/resources/fixtures/fmp/profile-iii.json` | NEW | prix en pence (3180 GBp) |
| `backend/src/test/java/.../price/FmpPriceProviderTest.java` | UPDATE | Ajouter test `fetch_convertsGBpToGbpFor3iGroup` |
| `frontend/src/app/app.routes.ts` | UPDATE | Route `/iii` avant `**` |
| `frontend/src/app/features/stocks/iii/iii.ts` | NEW | Standalone, imports: [AssetPage] |
| `frontend/src/app/features/stocks/iii/iii.html` | NEW | `<app-asset-page [assetId]="'iii'">` |
| `frontend/src/app/features/stocks/iii/iii.spec.ts` | NEW | Pattern brwm.spec.ts |
| `frontend/src/app/features/dashboard/dashboard.ts` | UPDATE | Signal, computed, sub, refresh |
| `frontend/src/app/features/dashboard/dashboard.html` | UPDATE | Carte `iii` après carte `o` |
| `frontend/src/app/features/dashboard/dashboard.spec.ts` | UPDATE | Mock + assertions iii |

### Données YAML déjà présentes (ne pas recréer)

Ces fichiers **existent déjà** (créés en Story 3.5) — ne pas les modifier sauf si les données sont incorrectes :

**`backend/src/main/resources/config/dividends/iii.yml`** (actuel) :
```yaml
asset-id: iii
forward-dividend: 0.58
forward-dividend-currency: GBP
frequency: semi-annual
avg-dividend-growth-10y: 12.5
history:
  - { year: 2024, amount: 0.54, currency: GBP }
  - { year: 2023, amount: 0.46, currency: GBP }
  - { year: 2022, amount: 0.38, currency: GBP }
```

**`backend/src/main/resources/config/fundamentals/iii.yml`** (actuel) :
```yaml
asset-id: iii
updated-at: 2025-05-14
source: "3i Group Annual Report 2025 (FY ending March 2025)"
metrics:
  trailing-pe: 18.2
  forward-pe: 16.8
  debt-leverage: "8%"
  dividend-payout-ratio: "22%"
```

> ⚠️ Le fichier `fundamentals/iii.yml` **ne contient pas de champ `nav-discount-premium`**, contrairement à `brwm.yml`. L'AC de l'épic mentionne "NAV/discount config" — mais les données actuelles ne l'ont pas. Le dev peut ajouter ce champ si la valeur est connue, sinon laisser tel quel (FundamentalsCard affiche uniquement ce qui est présent).

### Fixture WireMock — `profile-iii.json`

Créer à `backend/src/test/resources/fixtures/fmp/profile-iii.json` :
```json
[
  {
    "symbol": "III.L",
    "price": 3180.0,
    "marketCap": 30000000000,
    "changePercentage": 0.63,
    "volume": 850000,
    "currency": "GBp"
  }
]
```
Le test doit vérifier : `quote.price() == 31.80`, `quote.currency() == "GBP"`.

### Test FmpPriceProviderTest — Ajout

La classe a été refactorisée en Story 6.2 (code review) : WireMock est **statique** (`@BeforeAll`/`@AfterAll`) et les `AssetDefinition` sont des **champs statiques finaux**. Le pattern exact pour ajouter `iii` :

```java
private static final AssetDefinition iii = new AssetDefinition(
    "iii",
    "3i Group",
    AssetProvider.FMP,
    "III.L",
    "III",
    AssetType.TRUST,
    "GBP",
    new MarketHours(ZoneId.of("Europe/London"), LocalTime.of(8, 0), LocalTime.of(16, 35)),
    new SyncConfig(15, 6),
    null);
```

Et le test lui-même suit le pattern de `fetch_convertsGBpToGbpForLondonStocks` (déjà présent pour `brwm`).

### Assets Registry — Offset minutes

Chaque actif FMP a un `offset-minutes` distinct pour éviter la concurrence dans le scheduler :
- `inveb`: offset 0
- `brwm`: offset 3
- `o`: offset 0 (NYSE — heures différentes, pas de conflit)
- `iii`: offset **6** (LSE — même timezone que brwm, décalage nécessaire)

### Dashboard spec — Pattern à suivre

Le `dashboard.spec.ts` existant a **deux tests** : un sur les valeurs des computed signals, un sur les appels de service. Pour `iii` :
1. Ajouter `mockIiiDto` (assetId: 'iii', symbol: 'III', currentPrice: 31.80, priceChangePercentage24h: 0.63)
2. Câbler `getAsset` pour `'iii'` dans le mock
3. Ajouter assertions dans les deux tests (`iiiPrice()`, `iiiChange()`, `getAsset('iii')`)
4. Mettre à jour `toHaveBeenCalledTimes(4)` pour `getAsset` (était 3 après 6.2)

### Nommage — Attention

- Classe TypeScript : `Iii` (PascalCase — pas `III`)
- Sélecteur : `app-iii`
- Fichier : `iii.ts` / `iii.html` / `iii.spec.ts` (tout minuscule)
- AssetType Java : `TRUST` (même que BRWM, pas STOCK)

### References
- Story précédente : [6-2-realty-income-o.md](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/implementation-artifacts/stories/6-2-realty-income-o.md)
- Pattern composant reference : [brwm.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/features/stocks/brwm/brwm.ts)
- Pattern test composant : [brwm.spec.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/features/stocks/brwm/brwm.spec.ts)
- Dashboard actuel : [dashboard.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/features/dashboard/dashboard.ts)
- Routes actuelles : [app.routes.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/app.routes.ts)
- FmpPriceProviderTest : [FmpPriceProviderTest.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java)
- Épics : [epics.md](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/epics.md)
- Règles projet : [project-context.md](file:///c:/Users/chju/Desktop/Dashboard/docs/project-context.md)

## Dev Agent Record

### Agent Model Used
Gemini 3.5 Flash (Medium)

### Debug Log References
- `.\mvnw.cmd test` : 110 tests run successfully.
- `npm test` : 44 tests run successfully.

### Completion Notes List
- Registered `iii` asset in `assets-registry.yml` (LSE hours, interval 15 min, offset 6 min) for main and test.
- Created WireMock fixture `profile-iii.json` in pence (`GBp`).
- Added test case `fetch_convertsGBpToGbpFor3iGroup` in `FmpPriceProviderTest.java` verifying pence to pounds normalisation.
- Created `Iii` standalone Angular component, template, and spec.
- Registered `/iii` route as lazy-loaded in `app.routes.ts`.
- Integrated `iii` asset signal, computed properties, unsubscription, and refresh subscription in `dashboard.ts`.
- Added dashboard overview card for `iii` and mock/assertions in `dashboard.spec.ts`.

### File List
- `backend/src/main/resources/config/assets-registry.yml`
- `backend/src/test/resources/config/assets-registry.yml`
- `backend/src/test/resources/fixtures/fmp/profile-iii.json`
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/features/stocks/iii/iii.ts`
- `frontend/src/app/features/stocks/iii/iii.html`
- `frontend/src/app/features/stocks/iii/iii.spec.ts`
- `frontend/src/app/features/dashboard/dashboard.ts`
- `frontend/src/app/features/dashboard/dashboard.html`
- `frontend/src/app/features/dashboard/dashboard.spec.ts`

