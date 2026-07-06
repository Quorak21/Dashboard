---
story_id: 3.5
story_key: 3-5-fichiers-yaml-metier-pour-les-7-actifs
epic: 3
epic_name: Dividendes, fondamentaux et alertes trimestrielles
status: done
baseline_commit: 35ff7e019c6faf63eee0585a9bda731d54513a27
created: 2026-06-25
FRs:
  - FR-3
  - FR-5
dependencies:
  - 3.1 (Configuration dividendes YAML)
  - 3.2 (Configuration fondamentaux YAML)
  - 3.3 (Calcul estimatedYield backend)
  - 3.4 (Alerte fondamentaux stale et endpoint quarterly)
---

# Story 3.5: Fichiers YAML métier pour les 7 actifs

Status: done

## Story

En tant que Dokk,
je veux les fichiers de configuration YAML dividendes et fondamentaux remplis avec les données réelles des 7 actifs du registre,
afin que l'API retourne des blocs `dividends` et `fundamentals` réels pour chaque actif exposé.

## Acceptance Criteria

### AC-1: Fichiers YAML dividendes complets
**Given** les 7 actifs du registre (`inveb`, `brwm`, `iii`, `o`, `infr`, `chdiv`, `qqqe`)
**When** `GET /api/dashboard/asset/{assetId}` est appelé pour chaque actif
**Then** le bloc `dividends` retourné contient :
- `forwardDividend` (montant réel de dividende annuel par action/part, valeur non-null)
- `forwardDividendCurrency` (devise réelle : SEK, GBP, USD, CHF)
- `frequency` (`"annual"`, `"semi-annual"`, `"quarterly"`, `"monthly"`)
- `estimatedYield` calculé dynamiquement (non-null si prix disponible dans le cache)
- `avgDividendGrowth10Y` si disponible (nul pour les ETFs/trusts cycliques)
- `history` : au moins 3 années de dividendes annuels passés

### AC-2: Fichiers YAML fondamentaux complets
**Given** les 7 actifs du registre
**When** `GET /api/dashboard/asset/{assetId}` est appelé pour chaque actif
**Then** le bloc `fundamentals` retourné contient :
- `updatedAt` (date réelle de dernière mise à jour)
- `source` (référence à la source documentaire)
- `stale` calculé dynamiquement (non stocké dans le YAML)
- `metrics` : dictionnaire clé/valeur avec métriques pertinentes selon le type d'actif
- `topHoldings` : top 5 holdings sous-jacents (pour fonds/trusts/ETFs, si applicable)
- `sectorWeights` : répartition sectorielle en % (pour fonds/trusts/ETFs)

### AC-3: Données non-nulles pour les 7 actifs
**Given** les 12 fichiers YAML remplis (6 nouveaux + `inveb` déjà existant)
**When** `GET /api/dashboard/asset/{assetId}` est appelé pour chacun des 7 actifs
**Then** aucun actif ne retourne un bloc `dividends` ou `fundamentals` null

### AC-4: Pas de régression sur les endpoints existants
**Given** les fichiers YAML ajoutés
**When** `./mvnw.cmd test` est exécuté
**Then** 0 test fail — pas de régression (les tests existants ne dépendent pas des valeurs de données)

## Tasks / Subtasks

- [x] Fichiers YAML dividendes (AC-1)
  - [x] Créer `backend/src/main/resources/config/dividends/brwm.yml` (données BRWM ci-dessous)
  - [x] Créer `backend/src/main/resources/config/dividends/iii.yml` (données III ci-dessous)
  - [x] Créer `backend/src/main/resources/config/dividends/o.yml` (données O ci-dessous)
  - [x] Créer `backend/src/main/resources/config/dividends/infr.yml` (données INFR ci-dessous)
  - [x] Créer `backend/src/main/resources/config/dividends/chdiv.yml` (données CHDIV ci-dessous)
  - [x] Créer `backend/src/main/resources/config/dividends/qqqe.yml` (données QQQE ci-dessous)
- [x] Fichiers YAML fondamentaux (AC-2)
  - [x] Créer `backend/src/main/resources/config/fundamentals/brwm.yml`
  - [x] Créer `backend/src/main/resources/config/fundamentals/iii.yml`
  - [x] Créer `backend/src/main/resources/config/fundamentals/o.yml`
  - [x] Créer `backend/src/main/resources/config/fundamentals/infr.yml`
  - [x] Créer `backend/src/main/resources/config/fundamentals/chdiv.yml`
  - [x] Créer `backend/src/main/resources/config/fundamentals/qqqe.yml`
- [x] Vérification (AC-3 + AC-4)
  - [x] Exécuter `./mvnw.cmd test` — confirmer 0 fail
  - [x] Vérifier manuellement l'un des endpoints (ex: `GET /api/dashboard/asset/brwm`) si le backend est démarrable

### Review Findings

- [x] [Review][Decision] Somme des sector-weights de CHDIV incorrecte (85.7% au lieu de 100%) — Dans chdiv.yml, la somme des poids sectoriels donne 85.7% (22.1 + 20.8 + 19.3 + 14.6 + 8.9), laissant 14.3% non attribués. Les autres actifs utilisent une catégorie "Other" pour atteindre 100%.
- [x] [Review][Decision] Contradiction sur l'historique de dividendes d'inveb (2 ans vs 3 ans requis) — L'AC-1 demande "au moins 3 années de dividendes annuels passés" pour tous les actifs. La Dev Note 5 interdit de modifier inveb.yml, mais celui-ci ne contient que 2 années d'historique (2024 et 2023).
- [x] [Review][Patch] Utilisation de guillemets pour les dates dans les fichiers YAML fondamentaux [backend/src/main/resources/config/fundamentals/brwm.yml:2]
- [x] [Review][Patch] Absence de validation sur les valeurs vides/blanches et espaces dans l'ID d'actif [backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsConfiguration.java:75]
- [x] [Review][Patch] Absence de validation des pourcentages des Holdings/Secteurs [backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsConfiguration.java:75]
- [x] [Review][Patch] Cast de type brut dangereux dans les tests [backend/src/test/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsPropertiesTest.java:52]
- [x] [Review][Defer] Représentation incohérente des devises (codes ISO vs symboles) [backend/src/main/resources/config/fundamentals/brwm.yml:6] — deferred, pre-existing
- [x] [Review][Defer] Dépendance des tests unitaires envers les fichiers de données réels [backend/src/test/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsPropertiesTest.java:19] — deferred, pre-existing
- [x] [Review][Defer] Typage faible des métriques fondamentales [backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsProperties.java:11] — deferred, pre-existing

## Dev Notes

### ⚠️ Points critiques — lire avant de commencer

1. **`estimatedYield` et `stale` ne doivent JAMAIS être dans le YAML** — ils sont calculés dynamiquement par `ConfigurableAssetService.buildDividendsBlock()` et `QuarterlyReportAlertService.isStale()` respectivement. Les inclure ferait planter le binding Spring.

2. **Clé YAML = `asset-id` en minuscules** — le binding Spring `@ConfigurationProperties` mappe `app.dividends.{assetId}` où `assetId` est la clé dans la map. Les fichiers sont nommés `{assetId}.yml` et la clé dans le fichier doit correspondre exactement à l'ID du registre (ex: `brwm`, `iii`, `o`, etc.).

3. **Pas de test unitaire dédié** — cette story ne contient que des fichiers de données YAML. AC-4 est vérifié par les tests existants qui ne testent pas les valeurs, seulement la structure.

4. **`application.yml` importe les configs via** `spring.config.import: optional:classpath:config/assets-registry.yml`. Les fichiers `dividends/*.yml` et `fundamentals/*.yml` sont importés automatiquement car `AssetDividendsProperties` et `AssetFundamentalsProperties` utilisent `@ConfigurationProperties`. Vérifier dans `application.yml` ou `AssetDividendsProperties` comment les fichiers sont découverts.

5. **`inveb` existe déjà** — ne pas modifier `dividends/inveb.yml` et `fundamentals/inveb.yml`, ils sont déjà remplis et validés.

6. **Format de date YAML** : `updated-at: 2025-05-14` (format `YYYY-MM-DD`, sans guillemets). Spring le parse en `LocalDate`.

7. **Métriques comme String pour les valeurs avec %** : `debt-leverage: "35.8%"` (String), mais `trailing-pe: 43.5` (BigDecimal). Voir `AssetFundamentalsProperties.FundamentalsConfig` — la map `metrics` est `Map<String, Object>`, donc les deux types sont acceptés.

### Structure des fichiers à créer

```
backend/src/main/resources/config/
├── dividends/
│   ├── inveb.yml   ✅ EXISTS — ne pas toucher
│   ├── brwm.yml    ← CRÉER
│   ├── iii.yml     ← CRÉER
│   ├── o.yml       ← CRÉER
│   ├── infr.yml    ← CRÉER
│   ├── chdiv.yml   ← CRÉER
│   └── qqqe.yml    ← CRÉER
└── fundamentals/
    ├── inveb.yml   ✅ EXISTS — ne pas toucher
    ├── brwm.yml    ← CRÉER
    ├── iii.yml     ← CRÉER
    ├── o.yml       ← CRÉER
    ├── infr.yml    ← CRÉER
    ├── chdiv.yml   ← CRÉER
    └── qqqe.yml    ← CRÉER
```

### Vérifier le mécanisme d'import YAML

Avant de créer les fichiers, vérifier comment les fichiers `dividends/*.yml` sont importés.
Rechercher dans le projet : `AssetDividendsProperties`, `@ConfigurationProperties(prefix = "app.dividends")` et voir si `application.yml` utilise un glob import ou si `DividendsConfig` est une map simple.
Si les fichiers sont importés via `spring.config.import: optional:classpath:config/dividends/*.yml`, chaque fichier sera chargé séparément. Si non, les données doivent être dans un seul fichier. **Confirmer avant d'écrire les fichiers.**

### Données complètes pour chaque actif

Les données ci-dessous sont issues de sources officielles (rapports annuels, factsheets BlackRock/iShares/Invesco, investor relations) à date de juin 2025.

---

#### BRWM — BlackRock World Mining Trust (LSE: BRWM)
- Type: TRUST, Devise: GBP
- Source dividendes: BlackRock World Mining Trust Annual Report 2024
- Source fondamentaux: BlackRock World Mining Trust Annual Report 2024 (publié fév. 2025)

**dividends/brwm.yml :**
```yaml
app:
  dividends:
    brwm:
      asset-id: brwm
      forward-dividend: 0.185
      forward-dividend-currency: "GBP"
      frequency: "semi-annual"
      history:
        - year: 2024
          amount: 0.185
          currency: "GBP"
        - year: 2023
          amount: 0.130
          currency: "GBP"
        - year: 2022
          amount: 0.380
          currency: "GBP"
```
*Note: pas de `avg-dividend-growth-10y` — trust minier cyclique, croissance non significative*

**fundamentals/brwm.yml :**
```yaml
app:
  fundamentals:
    brwm:
      asset-id: brwm
      updated-at: 2025-02-28
      source: "BlackRock World Mining Trust Annual Report 2024"
      metrics:
        nav-discount-premium: "-13.5%"
        total-assets: "£1.4bn"
        management-fee: "0.70%"
      top-holdings:
        - name: "Glencore"
          weight-percent: 17.2
        - name: "BHP Group"
          weight-percent: 15.8
        - name: "Rio Tinto"
          weight-percent: 12.4
        - name: "Vale"
          weight-percent: 8.9
        - name: "Freeport-McMoRan"
          weight-percent: 7.1
      sector-weights:
        - sector: "Diversified Metals & Mining"
          weight-percent: 48.3
        - sector: "Copper"
          weight-percent: 22.1
        - sector: "Gold"
          weight-percent: 15.4
        - sector: "Iron Ore"
          weight-percent: 8.7
        - sector: "Other"
          weight-percent: 5.5
```

---

#### III — 3i Group PLC (LSE: III)
- Type: STOCK (Listed Private Equity), Devise: GBP
- Source dividendes: 3i Group Annual Report FY2025
- Source fondamentaux: 3i Group FY2025 Results (14 mai 2025)

**dividends/iii.yml :**
```yaml
app:
  dividends:
    iii:
      asset-id: iii
      forward-dividend: 0.58
      forward-dividend-currency: "GBP"
      frequency: "semi-annual"
      avg-dividend-growth-10y: 12.5
      history:
        - year: 2024
          amount: 0.54
          currency: "GBP"
        - year: 2023
          amount: 0.46
          currency: "GBP"
        - year: 2022
          amount: 0.38
          currency: "GBP"
```

**fundamentals/iii.yml :**
```yaml
app:
  fundamentals:
    iii:
      asset-id: iii
      updated-at: 2025-05-14
      source: "3i Group Annual Report 2025 (FY ending March 2025)"
      metrics:
        trailing-pe: 18.2
        forward-pe: 16.8
        debt-leverage: "8%"
        dividend-payout-ratio: "22%"
```
*Note: pas de `top-holdings` ni `sector-weights` pour III (private equity listed, le holding principal est Action à ~51% du NAV mais ce n'est pas un fonds avec holdings standards)*

---

#### O — Realty Income Corporation (NYSE: O)
- Type: STOCK (REIT), Devise: USD, dividende mensuel
- Source dividendes: Realty Income Investor Relations Q2 2025
- Source fondamentaux: Realty Income Q1 2025 Supplemental Operating Data (6 mai 2025)

**dividends/o.yml :**
```yaml
app:
  dividends:
    o:
      asset-id: o
      forward-dividend: 3.168
      forward-dividend-currency: "USD"
      frequency: "monthly"
      avg-dividend-growth-10y: 4.3
      history:
        - year: 2024
          amount: 3.072
          currency: "USD"
        - year: 2023
          amount: 2.976
          currency: "USD"
        - year: 2022
          amount: 2.808
          currency: "USD"
```

**fundamentals/o.yml :**
```yaml
app:
  fundamentals:
    o:
      asset-id: o
      updated-at: 2025-05-06
      source: "Realty Income Q1 2025 Supplemental Operating Data"
      metrics:
        trailing-pe: 43.5
        forward-pe: 38.2
        debt-leverage: "35.8%"
        dividend-payout-ratio: "76%"
```
*Note: Pour les REITs, le `dividend-payout-ratio` est l'AFFO payout ratio (métrique standard du secteur), pas le ratio net income classique. Le PE élevé est normal pour les REITs (comptabilité immobilière).*

---

#### INFR — iShares Global Infrastructure ETF (LSE: INFR)
- Type: ETF (Distributing), Devise: GBP
- Source dividendes: iShares INFR factsheet mai 2025
- Source fondamentaux: iShares INFR factsheet mai 2025

**dividends/infr.yml :**
```yaml
app:
  dividends:
    infr:
      asset-id: infr
      forward-dividend: 0.68
      forward-dividend-currency: "GBP"
      frequency: "semi-annual"
      history:
        - year: 2024
          amount: 0.65
          currency: "GBP"
        - year: 2023
          amount: 0.61
          currency: "GBP"
        - year: 2022
          amount: 0.55
          currency: "GBP"
```

**fundamentals/infr.yml :**
```yaml
app:
  fundamentals:
    infr:
      asset-id: infr
      updated-at: 2025-05-31
      source: "iShares Global Infrastructure ETF (INFR) factsheet May 2025"
      metrics:
        nav-discount-premium: "0.02%"
        total-assets: "$2.8bn"
        management-fee: "0.65%"
      top-holdings:
        - name: "NextEra Energy"
          weight-percent: 6.1
        - name: "Southern Company"
          weight-percent: 3.9
        - name: "Duke Energy"
          weight-percent: 3.7
        - name: "American Tower"
          weight-percent: 3.4
        - name: "Transurban Group"
          weight-percent: 3.1
      sector-weights:
        - sector: "Utilities"
          weight-percent: 61.2
        - sector: "Energy"
          weight-percent: 16.4
        - sector: "Industrials"
          weight-percent: 13.8
        - sector: "Real Estate"
          weight-percent: 5.3
        - sector: "Communication Services"
          weight-percent: 3.3
```

---

#### CHDIV — iShares Swiss Dividend ETF (SIX: CHDIV)
- Type: ETF (Distributing), Devise: CHF
- Source dividendes: iShares CHDIV factsheet avril 2025
- Source fondamentaux: iShares CHDIV factsheet avril 2025

**dividends/chdiv.yml :**
```yaml
app:
  dividends:
    chdiv:
      asset-id: chdiv
      forward-dividend: 1.85
      forward-dividend-currency: "CHF"
      frequency: "annual"
      history:
        - year: 2024
          amount: 1.78
          currency: "CHF"
        - year: 2023
          amount: 1.65
          currency: "CHF"
        - year: 2022
          amount: 1.52
          currency: "CHF"
```

**fundamentals/chdiv.yml :**
```yaml
app:
  fundamentals:
    chdiv:
      asset-id: chdiv
      updated-at: 2025-04-30
      source: "iShares Swiss Dividend ETF (CHDIV) factsheet April 2025"
      metrics:
        nav-discount-premium: "0.01%"
        total-assets: "CHF 1.2bn"
        management-fee: "0.15%"
      top-holdings:
        - name: "Nestlé"
          weight-percent: 14.8
        - name: "Novartis"
          weight-percent: 12.3
        - name: "Roche"
          weight-percent: 10.7
        - name: "Zurich Insurance"
          weight-percent: 7.4
        - name: "Swiss Re"
          weight-percent: 5.9
      sector-weights:
        - sector: "Consumer Staples"
          weight-percent: 22.1
        - sector: "Healthcare"
          weight-percent: 20.8
        - sector: "Financials"
          weight-percent: 19.3
        - sector: "Industrials"
          weight-percent: 14.6
        - sector: "Materials"
          weight-percent: 8.9
```

---

#### QQQE — Invesco NASDAQ 100 Equal Weight ETF (NASDAQ: QQQE)
- Type: ETF (quasi-accumulant — dividendes très faibles), Devise: USD
- Source dividendes: Invesco QQQE factsheet mai 2025
- Source fondamentaux: Invesco QQQE factsheet mai 2025
- **Note importante** : QQQE distribue un dividende minimal (~$0.28/an) car les sous-jacents sont des actions de croissance tech. L'ETF est majoritairement orienté croissance, pas revenus.

**dividends/qqqe.yml :**
```yaml
app:
  dividends:
    qqqe:
      asset-id: qqqe
      forward-dividend: 0.28
      forward-dividend-currency: "USD"
      frequency: "annual"
      history:
        - year: 2024
          amount: 0.25
          currency: "USD"
        - year: 2023
          amount: 0.21
          currency: "USD"
        - year: 2022
          amount: 0.18
          currency: "USD"
```

**fundamentals/qqqe.yml :**
```yaml
app:
  fundamentals:
    qqqe:
      asset-id: qqqe
      updated-at: 2025-05-31
      source: "Invesco NASDAQ 100 Equal Weight ETF (QQQE) factsheet May 2025"
      metrics:
        nav-discount-premium: "0.01%"
        total-assets: "$1.5bn"
        management-fee: "0.25%"
      top-holdings:
        - name: "Super Micro Computer"
          weight-percent: 1.3
        - name: "MicroStrategy"
          weight-percent: 1.2
        - name: "Palantir Technologies"
          weight-percent: 1.2
        - name: "Netflix"
          weight-percent: 1.1
        - name: "Meta Platforms"
          weight-percent: 1.1
      sector-weights:
        - sector: "Technology"
          weight-percent: 45.8
        - sector: "Consumer Discretionary"
          weight-percent: 18.3
        - sector: "Communication Services"
          weight-percent: 14.2
        - sector: "Healthcare"
          weight-percent: 7.9
        - sector: "Industrials"
          weight-percent: 5.6
        - sector: "Other"
          weight-percent: 8.2
```

### Patterns établis (Stories 3.1 à 3.4)

- Les fichiers YAML utilisent `kebab-case` pour les clés (Spring auto-binding)
- La clé de premier niveau est toujours `app.dividends.{assetId}` ou `app.fundamentals.{assetId}`
- La clé `asset-id` dans le fichier est redondante (utilisée pour lisibilité) mais doit correspondre à l'ID du registre
- Vérifier le pattern exact dans `inveb.yml` existant avant de créer les nouveaux fichiers

### Références

- [Assets Registry](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/resources/config/assets-registry.yml)
- [dividends/inveb.yml existant](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/resources/config/dividends/inveb.yml)
- [fundamentals/inveb.yml existant](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/resources/config/fundamentals/inveb.yml)
- [AssetDividendsProperties.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetDividendsProperties.java)
- [AssetFundamentalsProperties.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsProperties.java)
- [Architecture](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/architecture.md)

## Dev Agent Record

### Agent Model Used

Antigravity (Gemini 3.5 Flash)

### Debug Log References

### Completion Notes List

- Created 6 YAML files for asset dividends in backend/src/main/resources/config/dividends/
- Created 6 YAML files for asset fundamentals in backend/src/main/resources/config/fundamentals/
- Cleaned up binding format by removing outer app prefix hierarchy, matching the actual binder root-mapping behaviour
- Updated test assertions in AssetFundamentalsPropertiesTest to align with real brwm values rather than old stale mocks
- Ran 105 tests successfully using mvnw test

### File List

- `backend/src/main/resources/config/dividends/brwm.yml`
- `backend/src/main/resources/config/dividends/iii.yml`
- `backend/src/main/resources/config/dividends/o.yml`
- `backend/src/main/resources/config/dividends/infr.yml`
- `backend/src/main/resources/config/dividends/chdiv.yml`
- `backend/src/main/resources/config/dividends/qqqe.yml`
- `backend/src/main/resources/config/fundamentals/brwm.yml`
- `backend/src/main/resources/config/fundamentals/iii.yml`
- `backend/src/main/resources/config/fundamentals/o.yml`
- `backend/src/main/resources/config/fundamentals/infr.yml`
- `backend/src/main/resources/config/fundamentals/chdiv.yml`
- `backend/src/main/resources/config/fundamentals/qqqe.yml`
- `backend/src/test/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsPropertiesTest.java`

## Change Log

- 2026-06-25: Story implemented. Created 12 YAML files for dividends and fundamentals of 6 registry assets, updated tests to match real values, and verified the build.
- 2026-06-25: Story créée — contexte complet avec données financières réelles (Dokk)
