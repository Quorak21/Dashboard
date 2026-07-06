---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - docs/project-context.md
  - _bmad-output/planning-artifacts/prds/prd-Dashboard-2026-06-17/prd.md
  - _bmad-output/planning-artifacts/prds/prd-Dashboard-2026-06-17/addendum.md
  - _bmad-output/planning-artifacts/prds/prd-Dashboard-2026-06-17/.decision-log.md
  - _bmad-output/planning-artifacts/architecture.md
  - _bmad-output/planning-artifacts/research/spike-scrape-ubs-report-2026-06-17.md
workflowType: epics-and-stories
project_name: Dashboard
user_name: Dokk
date: '2026-06-17'
status: complete
completedAt: '2026-06-17'
revision: v1-multi-asset
---

# Dashboard — Epic Breakdown

## Overview

Ce document décompose le PRD v2.1 et l'architecture multi-actifs en epics et stories implémentables. Ordre de livraison : **cadre backend → sync → config métier → UI + InveB → scrape SIX → actifs FMP + portfolio**.

Brownfield : pas de starter template — Epic 1 Story 1 = registre YAML + `AssetRegistry`.

---

## Requirements Inventory

### Functional Requirements

```
FR-1: Registre d'actifs configurable (YAML, provider fmp|scrape, symboles, heures marché, sync)
FR-2: Endpoint REST GET /api/dashboard/{assetId} avec contrat unifié (prix, séries, blocs config)
FR-3: Persistance historique unifiée AssetDaily (7j) / AssetSnapshot (365j) par db-symbol
FR-4: Dividendes en config YAML + estimatedYield = forwardDividend / currentPrice × 100 (backend)
FR-5: Fondamentaux en config YAML + alerte si updatedAt > 90 jours
FR-6: Workflow MAJ config assistée LLM hors runtime (docs + restart Docker)
FR-7: Scrape Yahoo chart (yahoo-six-chart) pour CHDIV.SW et QQQE.SW, 10 min, heures SIX
FR-8: Page BlackRock World Mining (brwm) — FMP, GBP, trust NAV/discount config
FR-9: Page Realty Income (o) — REIT, USD, focus dividende mensuel config
FR-10: Page 3i Group (iii) — action LSE, NAV/discount config
FR-11: Page CHDIV — ETF SIX CHF, prix scrape, métriques config
FR-12: Page QQQE — ETF SIX USD, prix scrape, top holdings config
FR-13: Page INFR — ETF infrastructure FMP, donut sectoriel config
FR-14: Investor AB aligné sur modèle 3 couches (délégation service générique, plus de constantes front)
FR-15: Cartes overview pour tous les actifs (8 + HYPE), section ETF active
FR-16: Navbar et routes lazy pour tous les actifs sans régression HYPE/InveB
FR-17: Sync prix tierisée FMP 15 min (5 actifs) / scrape 10 min (2 ETF) / HYPE 10 min inchangé
FR-18: Observabilité compteurs FMP et scrape séparés en logs
```

### NonFunctional Requirements

```
NFR-1: Max 1 appel FMP /profile ou 1 scrape HTTP par actif par cycle sync
NFR-2: Écriture AssetDaily uniquement pendant heures de marché configurées
NFR-3: Aucun appel provider FMP/scrape sur GET /api/dashboard/* (lecture cache)
NFR-4: Quota FMP stable < 150 appels/jour ; WARN si > 200/jour (FR-18, SM-3)
NFR-5: Scrape SIX ~96 req/j max ; succès > 95 % en heures marché (SM-5)
NFR-6: RAM VPS limitée — cache AtomicReference par assetId, pas de Redis
NFR-7: Retry/timeout providers via ExternalCallExecutor existant
NFR-8: User-Agent scrape identifiable DashboardBot/1.0
NFR-9: Messages erreur utilisateur en français (toasts front)
NFR-10: HYPE inchangé — sync et endpoints existants non régressés
NFR-11: Pas de Flyway v1 — ddl-auto: update conservé
NFR-12: DTO backend records immutables ; BigDecimal pour calculs financiers
NFR-13: Contrat API : tout changement DTO répercuté dans frontend/src/app/core/models/
NFR-14: Ajout 9ᵉ actif FMP en < 2 h via config seule (SM-2)
```

### Additional Requirements

```
- Brownfield Spring Boot 4 / Angular 21 — pas de greenfield starter (ADR brownfield)
- Champ db-symbol distinct du symbole provider (INVE-B vs INVE-B.ST) — rétrocompat BD
- Un seul ConfigurableAssetService — pas de *Service × 7 (ADR-01)
- Interface PriceProvider + FmpPriceProvider + ScrapePriceProvider (ADR-04)
- Parser YahooSixChartParser seul en v1 — pas de factory extensible (non-goal)
- Reload config YAML : restart Docker v1, pas actuator (ADR-07)
- GET /api/dashboard/alerts/quarterly + flag fundamentals.stale inline DTO (ADR-08)
- InveBService façade @Deprecated temporaire puis suppression (ADR-01, M5)
- Tests : fixtures JSON Yahoo + WireMock FMP (ADR-15)
- Spike VPS : tester scrape depuis prod Docker avant Epic 5 (gate architecture §14)
- Steve → backend uniquement ; Alex/Picasso → frontend (project-context)
```

### UX Design Requirements

_Pas de document UX dédié — dérivé du PRD et project-context._

```
UX-DR1: Conserver thème dark & copper (palette tailwind.config.js existante)
UX-DR2: Composant PriceFreshnessBadge — états live / stale / marché fermé + timestamp
UX-DR3: Bandeau QuarterlyAlertsBanner sous navbar pour fondamentaux obsolètes
UX-DR4: Template AssetPage réutilisable — pas de copie page InveB × 6
UX-DR5: Cartes génériques DividendCard, FundamentalsCard, EtfMetricsCard, EtfSectorChart
UX-DR6: Section ETF overview — retirer opacity-40 et libellé COMING SOON
UX-DR7: Responsive mobile-first sur toutes nouvelles pages
UX-DR8: Badge « Prix source scrape (Yahoo) » sur cartes CHDIV/QQQE (FR-7)
```

### FR Coverage Map

| FR | Epic | Description |
|----|------|-------------|
| FR-1 | Epic 1 | Registre YAML + AssetRegistry |
| FR-2 | Epic 1 | AssetDto + GET /{assetId} |
| FR-3 | Epic 1, 2 | Persistance via service + snapshots job |
| FR-4 | Epic 3 | Dividendes YAML + yield calculé |
| FR-5 | Epic 3 | Fondamentaux YAML + alertes |
| FR-6 | Epic 3 | Documentation MAJ config |
| FR-7 | Epic 5 | Scrape Yahoo CHDIV/QQQE |
| FR-8 | Epic 6 | Page BRWM |
| FR-9 | Epic 6 | Page O |
| FR-10 | Epic 6 | Page III |
| FR-11 | Epic 5 | Page CHDIV |
| FR-12 | Epic 5 | Page QQQE |
| FR-13 | Epic 6 | Page INFR + sector chart |
| FR-14 | Epic 4 | Migration InveB |
| FR-15 | Epic 4, 6 | Overview cartes + bandeau alertes |
| FR-16 | Epic 4, 5, 6 | Routes lazy |
| FR-17 | Epic 2, 5 | Crons tierisés FMP/scrape/HYPE |
| FR-18 | Epic 2 | ProviderCallMetrics |

---

## Epic List

### Epic 1: Cadre backend multi-actifs
Dokk dispose d'un registre YAML et d'une API unifiée pour charger le prix FMP, l'historique BD et un DTO standard par `assetId` — sans dupliquer InveBService.
**FRs couverts :** FR-1, FR-2, FR-3 (base)

### Epic 2: Synchronisation prix et observabilité
Les prix FMP se synchronisent toutes les 15 min en heures marché, avec isolation des échecs et compteurs log — HYPE reste sur son cron 10 min.
**FRs couverts :** FR-3 (snapshots), FR-17 (FMP tier), FR-18

### Epic 3: Dividendes, fondamentaux et alertes trimestrielles
Dokk consulte rendement calculé, ratios et holdings depuis la config YAML ; le dashboard signale les fondamentaux > 90 jours.
**FRs couverts :** FR-4, FR-5, FR-6

### Epic 4: UI générique et migration Investor AB
InveB utilise le template partagé et les cartes génériques ; les constantes TS hardcodées disparaissent.
**FRs couverts :** FR-14, FR-15 (InveB), FR-16 (inveb)

### Epic 5: ETF SIX — prix scrape Yahoo
CHDIV et QQQE affichent un prix live SIX via Yahoo chart API, rafraîchi toutes les 10 min en session Zurich.
**FRs couverts :** FR-7, FR-11, FR-12, FR-17 (scrape tier)

### Epic 6: Portefeuille élargi — actions, ETF FMP et overview complet
Dokk voit BRWM, Realty Income, 3i, INFR et toutes les cartes overview ; la section ETF est active.
**FRs couverts :** FR-8, FR-9, FR-10, FR-13, FR-15, FR-16

---

## Epic 1: Cadre backend multi-actifs

Dokk dispose d'un registre YAML et d'une API unifiée pour charger le prix FMP, l'historique BD et un DTO standard par `assetId`.

### Story 1.1: Registre d'actifs YAML et AssetRegistry

En tant que Dokk,
je veux déclarer les actifs dans un fichier `assets-registry.yml`,
afin d'ajouter un futur titre sans créer un nouveau service Java.

**Acceptance Criteria:**

**Given** le fichier `config/assets-registry.yml` avec au minimum l'entrée `inveb` (provider fmp, symbol INVE-B.ST, db-symbol INVE-B)
**When** l'application Spring Boot démarre
**Then** `AssetRegistryProperties` charge toutes les entrées et `AssetRegistry.findById("inveb")` retourne la définition complète
**And** les champs `market-hours`, `sync.interval-minutes`, `sync.offset-minutes` et `currency` sont typés et validés au démarrage (erreur claire si entrée invalide)
**And** un test unitaire `AssetRegistryTest` vérifie le chargement depuis un YAML de test

_FRs : FR-1 · NFRs : NFR-14_

---

### Story 1.2: Interface PriceProvider et FmpPriceProvider

En tant que Dokk,
je veux un adaptateur FMP réutilisable pour tous les actifs du registre,
afin de centraliser les appels `/stable/profile`.

**Acceptance Criteria:**

**Given** `FMPClient` existant et une entrée registre `provider: fmp`
**When** `FmpPriceProvider.fetch(asset)` est appelé
**Then** il retourne un `PriceQuote` avec price, currency, marketCap, changePercent24h, volume et fetchedAt
**And** les erreurs provider passent par `ExternalCallExecutor` (NFR-7)
**And** `PriceProviderRegistry` résout le provider par `providerId` ("fmp")
**And** test WireMock avec fixture `profile-inveb.json` valide le mapping

_FRs : FR-1 · NFRs : NFR-1, NFR-7_

---

### Story 1.3: ConfigurableAssetService — cache, sync et historique

En tant que Dokk,
je veux un service unique qui gère cache, sync prix et séries BD,
afin de remplacer la logique dupliquée d'InveBService.

**Acceptance Criteria:**

**Given** un `assetId` présent dans le registre et des données historiques en BD sous `db-symbol`
**When** `syncPrice(assetId)` est appelé pendant heures marché
**Then** le service appelle `FmpPriceProvider`, met à jour `AtomicReference<AssetDto>` et écrit un point `AssetDaily` avec le `db-symbol`
**When** `getData(assetId)` est appelé (lecture HTTP ou job)
**Then** il retourne le cache sans appel FMP supplémentaire si le cache est peuplé (NFR-3)
**And** `historyPrices`/`historyDays` proviennent de `AssetSnapshot` (365 pts) et `livePrices`/`liveDays` de la session courante `AssetDaily` (≤ 144 pts) — même logique qu'InveBService
**And** en cas d'échec provider, retourne le cache stale avec `priceSource: CACHE` si disponible
**And** tests unitaires couvrent sync OK, fallback cache, skip écriture hors heures marché

_FRs : FR-1, FR-3 · NFRs : NFR-2, NFR-3, NFR-6_

---

### Story 1.4: Modèle AssetDto unifié

En tant que Dokk,
je veux un DTO JSON standard pour tous les actifs du registre,
afin que le front consomme un seul contrat.

**Acceptance Criteria:**

**Given** `ConfigurableAssetService.getData("inveb")` après sync réussie
**When** le DTO est sérialisé en JSON
**Then** la réponse contient `assetId`, `symbol` (db-symbol), `displayName`, `type`, `currency`, `currentPrice`, `marketCap`, `priceChangePercentage24h`, `totalVolume`, `lastRefresh`, `priceSource`, `marketStatus`, séries `history*` et `live*`
**And** les records Java sont immutables (NFR-12) ; blocs `dividends` et `fundamentals` nullables en attendant Epic 3
**And** enum `PriceSource` : FMP, SCRAPE, CACHE

_FRs : FR-2 · NFRs : NFR-12, NFR-13_

---

### Story 1.5: Endpoint GET /api/dashboard/{assetId}

En tant que Dokk,
je veux récupérer n'importe quel actif du registre via une route générique,
afin de préparer l'extension front.

**Acceptance Criteria:**

**Given** `DashboardController` existant avec `/hype` et `/inveb` inchangés
**When** `GET /api/dashboard/inveb` est appelé via la nouvelle route générique (ou route dédiée coexistence)
**Then** retourne `AssetDto` pour un `assetId` registre valide
**When** `GET /api/dashboard/unknown` est appelé
**Then** retourne 404 avec message générique (GlobalExceptionHandler)
**And** aucun appel FMP n'est déclenché par la requête HTTP (NFR-3)
**And** test MockMvc sur route générique

_FRs : FR-2 · NFRs : NFR-3, NFR-10_

---

## Epic 2: Synchronisation prix et observabilité

Les prix FMP se synchronisent toutes les 15 min en heures marché, avec isolation des échecs et compteurs log.

### Story 2.1: MarketHoursGuard multi-fuseaux

En tant que Dokk,
je veux que la sync et l'écriture intraday respectent les heures de chaque bourse,
afin de ne pas polluer AssetDaily la nuit ou le week-end.

**Acceptance Criteria:**

**Given** une entrée registre avec `market-hours.zone`, `open`, `close`
**When** `MarketHoursGuard.isOpen(asset)` est évalué un samedi ou hors fenêtre
**Then** retourne `false` et `marketStatus: CLOSED` dans le DTO
**And** tests couvrent Europe/Stockholm (inveb), Europe/London, America/New_York, Europe/Zurich

_FRs : FR-3, FR-17 · NFRs : NFR-2_

---

### Story 2.2: AssetSyncJob — tier FMP registry-driven

En tant que Dokk,
je veux que le job FMP itère le registre avec crons 15 min décalés,
afin de synchroniser les 5 actifs FMP sans hardcode InveBService.

**Acceptance Criteria:**

**Given** le registre contient inveb, brwm, o, iii, infr (peut n'avoir que inveb en phase initiale — autres entrées ajoutées Epic 6)
**When** `syncFmpAssets` s'exécute selon cron `0 {offset}/15 * * * ?`
**Then** chaque actif fmp est synchronisé via `ConfigurableAssetService.syncPrice` uniquement si `MarketHoursGuard.isOpen`
**And** un échec sur un actif est loggé ERROR sans bloquer les autres (pattern sendHypeSnapshot/sendInveBSnapshot)
**And** `autoSync` HYPE reste sur cron 10 min séparé inchangé (NFR-10)

_FRs : FR-17 · NFRs : NFR-1, NFR-10_

---

### Story 2.3: ProviderCallMetrics et logs observabilité

En tant que Dokk,
je veux voir en logs le nombre d'appels FMP et scrape du jour,
afin de surveiller les quotas (FR-18).

**Acceptance Criteria:**

**Given** des sync FMP et scrape exécutées
**When** une heure s'écoule ou à chaque sync batch
**Then** log INFO `FMP calls today: N` et `Scrape calls today: M`
**When** N > 200
**Then** log WARN quota FMP (NFR-4)
**When** taux échec scrape > 20 % sur 1 h
**Then** log WARN scrape_fail_rate
**And** compteurs reset à minuit UTC
**And** test unitaire `ProviderCallMetricsTest`

_FRs : FR-18 · NFRs : NFR-4_

---

### Story 2.4: Snapshots quotidiens pour actifs registre

En tant que Dokk,
je veux des clôtures journalières en BD pour tous les actifs du registre,
afin d'alimenter les graphiques annuels (365 pts).

**Acceptance Criteria:**

**Given** des points `AssetDaily` existants pour un `db-symbol`
**When** `sendDailySnapshotToDb` s'exécute à minuit UTC
**Then** une boucle sur le registre (hors hype) crée `AssetSnapshot` avec prix du dernier daily si non null
**And** HYPE conserve sa logique snapshot dédiée existante
**And** échec par symbole isolé (try/catch par actif)
**And** `cleanDB` (7j daily, 365j snapshot) inchangé

_FRs : FR-3 · NFRs : NFR-10_

---

## Epic 3: Dividendes, fondamentaux et alertes trimestrielles

Dokk consulte rendement calculé, ratios et holdings depuis la config YAML ; le dashboard signale les fondamentaux > 90 jours.

### Story 3.1: Configuration dividendes YAML

En tant que Dokk,
je veux maintenir l'historique dividendes en fichiers YAML versionnés,
afin de mettre à jour sans redéployer du code.

**Acceptance Criteria:**

**Given** `config/dividends/{assetId}.yml` avec `forward-dividend`, `history[]`, `frequency`
**When** l'application démarre
**Then** `AssetDividendsProperties` charge un map par `assetId`
**And** fichier exemple `dividends/inveb.yml` conforme addendum architecture
**And** test chargement YAML + entrée manquante = bloc dividends null dans DTO

_FRs : FR-4, FR-6 · ADR-06, ADR-07_

---

### Story 3.2: Configuration fondamentaux YAML

En tant que Dokk,
je veux des ratios, NAV, TER et holdings en config avec date de fraîcheur,
afin de déclencher l'alerte trimestrielle.

**Acceptance Criteria:**

**Given** `config/fundamentals/{assetId}.yml` avec `updated-at`, `source`, `metrics`, optionnels `top-holdings`, `sector-weights`
**When** l'application démarre
**Then** `AssetFundamentalsProperties` charge le map par `assetId`
**And** fichiers exemples `inveb.yml` et `brwm.yml` présents
**And** champ `source` exposé dans DTO pour label « Source : rapport »

_FRs : FR-5, FR-6_

---

### Story 3.3: Calcul estimatedYield backend

En tant que Dokk,
je veux que le rendement dividende soit calculé sur le prix live,
afin de ne plus hardcoder 6 SEK côté front.

**Acceptance Criteria:**

**Given** `forwardDividend` en config et `currentPrice` du cache sync
**When** `AssetDto` est construit
**Then** `dividends.estimatedYield = forwardDividend / currentPrice × 100` en BigDecimal scale 2 (NFR-12)
**When** `currentPrice` est null
**Then** `estimatedYield` est null (pas de division par zéro)
**And** test unitaire avec prix et forward connus

_FRs : FR-4 · NFRs : NFR-12_

---

### Story 3.4: Alerte fondamentaux stale et endpoint quarterly

En tant que Dokk,
je veux être alerté quand les fondamentaux n'ont pas été mis à jour depuis 90 jours,
afin de planifier ma revue trimestrielle (UJ-4).

**Acceptance Criteria:**

**Given** `fundamentals.updatedAt` > 90 jours (seuil configurable `app.alerts.fundamentals-stale-days`)
**When** `QuarterlyReportAlertService.getStaleAssets()` est appelé
**Then** retourne la liste `{ assetId, displayName, updatedAt, daysStale }`
**When** `GET /api/dashboard/alerts/quarterly` est appelé
**Then** retourne JSON des alertes
**And** chaque `AssetDto` inclut `fundamentals.stale: boolean`
**And** test seuil 89j (pas stale) vs 91j (stale)

_FRs : FR-5_

---

### Story 3.5: Fichiers YAML métier pour les 7 actifs

En tant que Dokk,
je veux les fichiers dividends et fundamentals pour tous les actifs du catalogue,
afin d'avoir des pages complètes dès leur activation.

**Acceptance Criteria:**

**Given** le catalogue PRD (inveb, brwm, o, iii, chdiv, qqqe, infr)
**When** les fichiers YAML sont présents dans `config/dividends/` et `config/fundamentals/`
**Then** chaque `assetId` a au minimum forward-dividend et updated-at renseignés (valeurs placeholder documentées en commentaire si données non encore saisies)
**And** README ou commentaire dans addendum rappelle workflow MAJ trimestrielle LLM hors app (FR-6)

_FRs : FR-4, FR-5, FR-6, FR-8 à FR-13 (données config)_

---

## Epic 4: UI générique et migration Investor AB

InveB utilise le template partagé et les cartes génériques ; les constantes TS hardcodées disparaissent.

### Story 4.1: Modèle AssetDto front et DashboardApiService

En tant que Dokk,
je veux les types TypeScript miroir du backend,
afin de consommer l'API unifiée depuis Angular.

**Acceptance Criteria:**

**Given** `frontend/src/app/core/models/asset.dto.ts` avec interfaces alignées sur `AssetDto` Java
**When** `DashboardApiService.getAsset(assetId)` est appelé
**Then** `GET /api/dashboard/{assetId}` est invoqué et retourne `Observable<AssetDto | null>`
**And** erreur HTTP → toast français + `of(null)` (NFR-9, project-context)
**And** `getQuarterlyAlerts()` ajouté pour Epic 3 endpoint

_FRs : FR-2, FR-14 · NFRs : NFR-9, NFR-13 · UX : UX-DR4_

---

### Story 4.2: Composants génériques DividendCard et FundamentalsCard

En tant que Dokk,
je veux des cartes dividendes et fondamentaux alimentées par le DTO,
afin de réutiliser le même composant sur tous les actifs.

**Acceptance Criteria:**

**Given** un `AssetDto` avec blocs `dividends` et `fundamentals` peuplés
**When** `DividendCard` et `FundamentalsCard` sont rendus
**Then** ils affichent historique, forward, estimatedYield %, métriques et top holdings depuis le DTO — zéro constante métier hardcodée
**And** état vide (`hasData=false`) affiche tirets comme pattern InveB actuel
**And** tests Vitest colocalisés avec mocks DTO

_FRs : FR-4, FR-5, FR-14 · UX : UX-DR1, UX-DR5 · SM-4_

---

### Story 4.3: Template AssetPage et PriceFreshnessBadge

En tant que Dokk,
je veux une page actif réutilisable avec badge de fraîcheur du prix,
afin de voir si le cours est live, stale ou marché fermé.

**Acceptance Criteria:**

**Given** `AssetPage` avec input `assetId`
**When** la page charge via `getAsset(assetId)`
**Then** affiche `PriceFreshnessBadge`, carte prix principale, graphiques annuel/intraday existants (`PriceChart`, `DailyChart`)
**And** badge états : live (FMP/SCRAPE récent), stale (CACHE ou age > 2× interval), closed (marketStatus CLOSED)
**And** thème dark/copper respecté (UX-DR1, UX-DR2)
**And** responsive mobile-first (UX-DR7)

_FRs : FR-14, FR-16 · UX : UX-DR2, UX-DR4, UX-DR7_

---

### Story 4.4: Migration page InveB vers AssetPage

En tant que Dokk,
je veux qu'Investor AB utilise le nouveau template sans régression,
afin de valider le cadre sur l'actif existant.

**Acceptance Criteria:**

**Given** route `/inveb` lazy existante
**When** Dokk ouvre la page InveB
**Then** elle délègue à `AssetPage` avec `assetId='inveb'`
**And** `INVEB_FUNDAMENTALS_METRICS` et constantes dividendes supprimées des fichiers `inveb-*-card`
**And** tests `inveb.spec.ts` mis à jour avec mock `AssetDto`
**And** graphiques et prix SEK inchangés visuellement

_FRs : FR-14, FR-16 · SM-4_

---

### Story 4.5: Façade InveBService et YAML inveb

En tant que Dokk,
je veux qu'InveBService délègue au service générique,
afin de préserver les tests et endpoints legacy pendant la transition.

**Acceptance Criteria:**

**Given** `config/dividends/inveb.yml` et `config/fundamentals/inveb.yml` peuplés
**When** `InveBService.getData()` est appelé
**Then** délègue à `ConfigurableAssetService.getData("inveb")` et mappe vers `InveBDto` OU endpoint `/inveb` retourne `AssetDto` (décision impl : mapping minimal pour tests existants)
**And** `InveBService` annoté `@Deprecated`
**And** sync InveB passe par registre (plus d'appel FMP direct dans InveBService)
**And** `InveBServiceTest` vert ou migré vers `ConfigurableAssetServiceTest`

_FRs : FR-14 · NFRs : NFR-10_

---

### Story 4.6: Carte overview InveB et bandeau alertes

En tant que Dokk,
je veux la carte overview InveB via le nouveau modèle et un bandeau d'alertes fondamentaux,
afin de voir la santé du portefeuille dès l'accueil.

**Acceptance Criteria:**

**Given** le dashboard overview
**When** Dokk ouvre `/`
**Then** carte InveB consomme `getAsset('inveb')` ou pattern existant étendu
**And** `QuarterlyAlertsBanner` affiche les actifs stale depuis `getQuarterlyAlerts()` (UX-DR3)
**And** texte français : « Fondamentaux à vérifier : … »

_FRs : FR-15 (partiel) · UX : UX-DR3_

---

## Epic 5: ETF SIX — prix scrape Yahoo

CHDIV et QQQE affichent un prix live SIX via Yahoo chart API.

### Story 5.1: YahooChartClient et YahooSixChartParser

En tant que Dokk,
je veux parser le prix depuis l'API Yahoo chart JSON,
afin de couvrir CHDIV.SW et QQQE.SW sans lib yfinance.

**Acceptance Criteria:**

**Given** fixture `yahoo-chdiv.json` et `yahoo-qqqe.json` capturées au spike
**When** `YahooSixChartParser.parse(json, asset)` est appelé
**Then** extrait `regularMarketPrice` et `currency` depuis `chart.result[0].meta`
**And** `YahooChartClient` utilise RestClient + User-Agent DashboardBot/1.0 (NFR-8) + ExternalCallExecutor timeout 15s retry 2
**And** tests : parse OK, résultat vide → exception, fixture empty

_FRs : FR-7 · NFRs : NFR-7, NFR-8_

---

### Story 5.2: ScrapePriceProvider et intégration registre

En tant que Dokk,
je veux un provider scrape branché sur le registre,
afin que ConfigurableAssetService traite CHDIV et QQQE comme les actifs FMP.

**Acceptance Criteria:**

**Given** entrées registre `provider: scrape`, `scrape-parser: yahoo-six-chart`
**When** `ScrapePriceProvider.fetch(asset)` est appelé
**Then** délègue au parser yahoo-six-chart et retourne `PriceQuote`
**When** échec HTTP ou parse
**Then** ConfigurableAssetService conserve cache avec `priceSource: CACHE` et log WARN

_FRs : FR-7 · NFRs : NFR-1, NFR-5_

---

### Story 5.3: Job sync scrape 10 min heures SIX

En tant que Dokk,
je veux synchroniser CHDIV et QQQE toutes les 10 min en session SIX uniquement,
afin de respecter ~96 req/j (FR-17).

**Acceptance Criteria:**

**Given** registre chdiv et qqqe avec market-hours Europe/Zurich 09:00–17:30
**When** `syncScrapeAssets` cron `0 0/10 * * * ?` zone Zurich s'exécute un jour ouvré ouvert
**Then** sync les actifs scrape du registre et incrémente `ProviderCallMetrics.scrape`
**When** hors heures ou week-end
**Then** aucun appel Yahoo
**And** isolation échec par actif

_FRs : FR-7, FR-17 · NFRs : NFR-5_

---

### Story 5.4: Pages et overview CHDIV et QQQE

En tant que Dokk,
je veux consulter les deux ETF SIX avec prix scrape et métriques config,
afin de suivre mon exposition dividendes suisse et Nasdaq 100.

**Acceptance Criteria:**

**Given** YAML dividends/fundamentals chdiv et qqqe + sync scrape active
**When** Dokk ouvre `/chdiv` et `/qqqe`
**Then** routes lazy `features/etf/chdiv` et `features/etf/qqqe` rendent `AssetPage`
**And** overview affiche cartes CHDIV (CHF) et QQQE (USD) avec badge scrape Yahoo (UX-DR8)
**And** `EtfMetricsCard` affiche TER/AUM depuis fundamentals config

_FRs : FR-11, FR-12, FR-15, FR-16 · UX : UX-DR6, UX-DR8_

---

### Story 5.5: Gate spike VPS scrape (pré-implémentation)

En tant que Dokk,
je veux valider l'accès Yahoo chart depuis le VPS prod,
afin de confirmer le gate avant mise en prod scrape.

**Acceptance Criteria:**

**Given** script `spike-scrape-ubs.ps1` ou équivalent curl
**When** exécuté depuis le conteneur Docker prod
**Then** CHDIV.SW et QQQE.SW retournent `regularMarketPrice` JSON valide
**And** résultat documenté dans journal ou note impl (gate architecture §14)
**When** échec réseau
**Then** documenter plan B (prix manuel YAML temporaire)

_FRs : FR-7 · Gate architecture — non bloquant dev local_

---

## Epic 6: Portefeuille élargi — actions, ETF FMP et overview complet

Dokk voit BRWM, Realty Income, 3i, INFR et toutes les cartes overview.

### Story 6.1: BlackRock World Mining (brwm)

En tant que Dokk,
je veux suivre BRWM avec NAV/discount et dividendes config,
afin de monitorer mon trust minière LSE.

**Acceptance Criteria:**

**Given** entrée registre brwm (FMP BRWM.L, db-symbol BRWM, GBP, London hours) + YAML div/fund
**When** Dokk ouvre `/brwm`
**Then** `AssetPage` affiche prix FMP, graphiques, carte dividende yield calculé, fondamentaux NAV/discount
**And** carte overview BRWM sur dashboard
**And** sync FMP 15 min via job Epic 2

_FRs : FR-8, FR-15, FR-16_

---

### Story 6.2: Realty Income (o)

En tant que Dokk,
je veux suivre Realty Income en USD avec focus dividende mensuel,
afin de voir mon rendement REIT calculé sur prix live.

**Acceptance Criteria:**

**Given** registre `o` (symbol O, NYSE hours, USD) + YAML dividende mensuel
**When** page `/o` et overview carte sont accessibles
**Then** DividendCard met en avant fréquence mensuelle et historique config

_FRs : FR-9, FR-15, FR-16_

---

### Story 6.3: 3i Group (iii)

En tant que Dokk,
je veux suivre 3i avec valorisation NAV/discount,
afin d'évaluer la décote du trust private equity.

**Acceptance Criteria:**

**Given** registre iii (III.L, GBP, London) + YAML fondamentaux NAV
**When** page `/iii` accessible
**Then** FundamentalsCard affiche NAV/discount depuis config

_FRs : FR-10, FR-16_

---

### Story 6.4: iShares Global Infrastructure (infr) et EtfSectorChart

En tant que Dokk,
je veux voir INFR avec répartition sectorielle,
afin de comprendre l'exposition infrastructure.

**Acceptance Criteria:**

**Given** registre infr (INFR.L, USD) + YAML `sector-weights`
**When** page `/infr` rendue
**Then** composant `EtfSectorChart` (donut Chart.js) affiche les poids secteurs depuis DTO
**And** `EtfMetricsCard` affiche TER et AUM

_FRs : FR-13, FR-16 · UX : UX-DR5_

---

### Story 6.5: Overview complet et section ETF active

En tant que Dokk,
je veux voir toutes les cartes actifs sur l'accueil avec la section ETF débloquée,
afin d'avoir une vue portefeuille en un coup d'œil (UJ-1).

**Acceptance Criteria:**

**Given** les 7 actifs registre + HYPE opérationnels
**When** Dokk ouvre `/`
**Then** 8 cartes hors HYPE affichent prix, Δ24h, devise et lien détail
**And** section ETF sans `opacity-40` ni « COMING SOON » (UX-DR6)
**And** refresh front ~3 min pattern existant conservé

_FRs : FR-15 · UX : UX-DR6 · SM-1_

---

### Story 6.6: Navigation complète et nettoyage legacy

En tant que Dokk,
je veux une navbar à jour et la suppression du code mort InveB,
afin de clôturer la migration brownfield.

**Acceptance Criteria:**

**Given** toutes les routes `/inveb`, `/brwm`, `/o`, `/iii`, `/chdiv`, `/qqqe`, `/infr`, `/hype`
**When** navbar est affichée
**Then** tous les liens fonctionnent ; 404 → redirect `/` inchangé
**And** `InveBService` façade supprimée si `GET /inveb` sert `AssetDto` directement
**And** `mvn test` et `ng test` verts

_FRs : FR-16, FR-14 · NFRs : NFR-10_

---

## Validation finale (Step 4)

### Couverture FR

| FR | Stories |
|----|---------|
| FR-1 | 1.1, 1.2 |
| FR-2 | 1.4, 1.5, 4.1 |
| FR-3 | 1.3, 2.4 |
| FR-4 | 3.1, 3.3, 3.5, 4.2 |
| FR-5 | 3.2, 3.4, 3.5, 4.2 |
| FR-6 | 3.1, 3.2, 3.5 |
| FR-7 | 5.1, 5.2, 5.3, 5.5 |
| FR-8 | 6.1 |
| FR-9 | 6.2 |
| FR-10 | 6.3 |
| FR-11 | 5.4 |
| FR-12 | 5.4 |
| FR-13 | 6.4 |
| FR-14 | 4.1–4.5, 6.6 |
| FR-15 | 4.6, 5.4, 6.1–6.5 |
| FR-16 | 4.3, 4.4, 5.4, 6.1–6.6 |
| FR-17 | 2.2, 5.3 |
| FR-18 | 2.3 |

✅ 18/18 FR couverts.

### Couverture UX-DR

| UX-DR | Stories |
|-------|---------|
| UX-DR1 | 4.2, 4.3 |
| UX-DR2 | 4.3 |
| UX-DR3 | 4.6 |
| UX-DR4 | 4.1, 4.3 |
| UX-DR5 | 4.2, 6.4 |
| UX-DR6 | 5.4, 6.5 |
| UX-DR7 | 4.3 |
| UX-DR8 | 5.4 |

✅ 8/8 UX-DR couverts.

### Dépendances

- Epic 2 dépend de Epic 1 (service + registre) — ✅ Epic 1 standalone livrable avec inveb seul
- Epic 3 peut démarrer après 1.4 (DTO) — stories 3.x enrichissent DTO sans bloquer Epic 2
- Epic 4 dépend de Epic 1 + 3 (blocs div/fund dans DTO)
- Epic 5 dépend de Epic 1 + 2 (job scrape) + gate 5.5 recommandé avant prod
- Epic 6 dépend de Epic 1–4 ; actifs FMP indépendants entre eux (6.1–6.4 parallélisables)

### Ordre d'implémentation recommandé

```
1.1 → 1.2 → 1.3 → 1.4 → 1.5 → 2.1 → 2.2 → 2.3 → 2.4
→ 3.1 → 3.2 → 3.3 → 3.4 → 3.5
→ 4.1 → 4.2 → 4.3 → 4.4 → 4.5 → 4.6
→ 5.1 → 5.2 → 5.3 → 5.5 (gate) → 5.4
→ 6.1 → 6.2 → 6.3 → 6.4 → 6.5 → 6.6
```

**Première story dev :** `1.1 — Registre d'actifs YAML et AssetRegistry` (`bmad-dev-story`).

---

_Epics & Stories v1 — PRD v2.1 + architecture M0–M5 — 6 epics, 28 stories._
