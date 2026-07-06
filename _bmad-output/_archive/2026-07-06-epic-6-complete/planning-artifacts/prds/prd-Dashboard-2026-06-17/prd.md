---
title: Dashboard — Extension multi-actifs (actions, ETF, InveB live)
status: final
created: 2026-06-17
updated: 2026-06-17
revision: v2.1-yahoo-scrape
---

# PRD : Dashboard — Extension multi-actifs

*Extension brownfield du dashboard personnel Angular 21 + Spring Boot 4 pour suivre 4 nouveaux actifs et moderniser Investor AB — modèle données en 3 couches : prix live, dividendes en config, fondamentaux en config + alerte.*

## 0. Document Purpose

Ce PRD s'adresse à Dokk (builder unique) et aux workflows BMad en aval (`bmad-create-architecture`, `bmad-create-epics-and-stories`, `bmad-dev-story`). Il décrit les capacités produit — pas l'implémentation détaillée (voir `addendum.md`). Le vocabulaire est ancré au §3 Glossary. Les inférences non confirmées portent le tag `[ASSUMPTION]`.

**Inputs consultés :** `docs/project-context.md`, code existant HYPE / InveB, spike providers (`scripts/spike-providers.ps1`), registre symboles (addendum).

**Changement majeur v2 (2026-06-17) :** abandon des fondamentaux/dividendes live via API payante (FMP dividends/ratios, Finnhub). Modèle **fmp | scrape | config** — voir `.decision-log.md` D-11 à D-17.

**v2.1 (2026-06-17) :** spike scrape — parser **`yahoo-six-chart`** pour CHDIV/QQQE (D-21). Pages UBS non parseables (React fundgate). Pas de lib `yfinance`.

---

## 1. Vision

Le Dashboard suit aujourd'hui HYPE (crypto) et Investor AB (action suédoise). Dokk l'utilise en production personnelle et souhaite y ajouter quatre actifs supplémentaires — mines, REIT, private equity et ETF infrastructure — **sans coder chaque ajout manuellement**. Le pattern InveB (prix live + historique BD) et la section ETF « COMING SOON » du front doivent devenir un **cadre reproductible** : configurer un actif (symbole, type, provider prix, fuseau horaire) suffit pour obtenir cartes overview, page détail et graphiques annuel/intraday.

Les **données métier** (dividendes historiques, ratios, TER, holdings) vivent en **configuration YAML** maintenue manuellement (~tous les 3 mois, assistée par LLM quand une alerte rapport trimestriel le signale). Seul le **prix de la carte principale** est live — via FMP (5 actifs validés au spike) ou scrape ciblé (2 ETF SIX non couverts par FMP).

**Pourquoi maintenant :** l'infra providers (`FMPClient`, `AssetDaily`, `AssetSnapshot`, `AssetSyncJob`) est mature ; le spike FMP/Finnhub a tranché : FMP `/stable/profile` suffit pour le prix de 5 titres (InveB, BRWM, O, III, INFR) ; les endpoints FMP dividendes/ratios sont hors plan (402). Le modèle 3 couches reste dans le quota FMP Starter sans provider payant supplémentaire.

---

## 2. Target User

### 2.1 Jobs To Be Done

- **Consulter** en un coup d'œil la santé de tout le portefeuille (prix live, variation 24h, devise correcte).
- **Approfondir** un actif : historique 1 an, intraday session, dividendes (historique config + rendement calculé), ratios clés (config).
- **Comparer** des profils différents (crypto riche vs action vs ETF) sans quitter l'UI dark/copper existante.
- **Ajouter** un futur actif par configuration (registre YAML), pas par copier-coller de service Java.
- **Être alerté** quand les fondamentaux d'un actif sont probablement obsolètes (fenêtre rapport trimestriel) et les mettre à jour via workflow LLM.

### 2.2 Non-Users (v1)

- Autres utilisateurs / multi-tenant.
- Trading, ordres, alertes push.
- Conversion devises temps réel (affichage en devise de cotation uniquement).

### 2.3 Key User Journeys

**UJ-1. Dokk ouvre le dashboard le matin.**
Dokk ouvre `dashboard.dokkcorp.ch` sur mobile. La page overview affiche HYPE, InveB, BRWM, Realty Income, 3i, et les 3 ETF avec prix live et Δ24h. Il tape sur « Realty Income » → page détail avec graphique annuel USD, badge « marché ouvert », carte dividendes (historique config + rendement % calculé sur prix live). Il ferme l'onglet en < 2 min.

**UJ-2. Dokk vérifie le discount NAV de BlackRock World Mining.**
Après clôture LSE, Dokk consulte BRWM : prix live FMP, prime/discount % et NAV depuis la config fondamentaux (MAJ au dernier rapport). Le rendement dividende affiché = dividende forward config / prix live.

**UJ-3. Dokk suit QQQE sur SIX.**
Pendant la session SIX, le prix QQQE se rafraîchit toutes les 10 min via Yahoo chart API (`QQQE.SW`). Si l'appel échoue, la carte affiche le dernier prix connu + timestamp « il y a X min ». Les métriques ETF (TER, top holdings) viennent de la config.

**UJ-4. Dokk met à jour les fondamentaux après publication de résultats.**
Une alerte dashboard signale « Rapport Q1 probable — vérifier fondamentaux InveB ». Dokk extrait les chiffres du rapport, passe par son workflow LLM, et met à jour le YAML config. L'alerte disparaît jusqu'au prochain trimestre.

---

## 3. Glossary

- **Actif** — Instrument financier suivi par le Dashboard (crypto, Action, REIT, ETF). Identifié par un `assetId` kebab-case (`brwm`, `chdiv`).
- **Action** — Titre coté (style InveB) : prix live, historique BD, cartes dividendes/fondamentaux depuis config.
- **ETF** — Fonds indiciel coté : hérite du pattern Action + métriques TER, AUM, pondérations (config).
- **REIT** — Action à logique immobilière (Realty Income) : pattern Action enrichi de métriques dividende.
- **Couche prix** — Données live pour la carte principale et les graphiques (provider `fmp` ou `scrape`).
- **Couche dividendes** — Historique et montants en config YAML ; **rendement %** calculé backend = dividende forward config ÷ prix live.
- **Couche fondamentaux** — Ratios, NAV, TER, holdings en config YAML ; fraîcheur suivie par **alerte rapport trimestriel**.
- **Price Provider** — Implémentation backend : `fmp` (FMP `/stable/profile`) ou `scrape` (HTML émetteur, Jsoup).
- **FMP** — Financial Modeling Prep ; utilisé **uniquement** pour le prix live des 5 actifs validés au spike.
- **Scrape Provider** — Job backend qui récupère le prix SIX via HTTP JSON (parser `yahoo-six-chart`) ; réservé à `chdiv` et `qqqe`. Pas de lib `yfinance`.
- **AssetDaily** — Entité JPA : points intraday (≤ 7 jours) pour graphique session.
- **AssetSnapshot** — Entité JPA : clôture journalière (≤ 365 jours) pour graphique annuel.
- **Sync T0** — Tier prix : FMP 15 min (5 actifs) ou scrape 10 min (2 ETF), heures marché uniquement.
- **Alerte rapport trimestriel** — Bandeau ou badge dashboard listant les actifs dont la config fondamentaux dépasse le seuil d'âge (défaut : 90 jours).
- **Pattern HYPE** — Page crypto riche — **non applicable** aux nouveaux actifs.
- **Pattern InveB** — Page action : `AssetMainCard`, `PriceChart`, `DailyChart`, cartes métier.
- **Métrique live** — Prix et séries historiques ; **ne couvre pas** dividendes ni fondamentaux dans ce PRD.

---

## 4. Features

### 4.1 Cadre générique multi-provider

**Description :** Un registre d'actifs (YAML) décrit `provider` (`fmp` | `scrape`), symbole ou URL scrape, type, devise, fuseau et décalage de sync. Un service backend générique charge le prix, alimente `AssetDaily`/`AssetSnapshot`, expose `GET /api/dashboard/{assetId}` et un DTO front miroir. Le front enregistre route lazy + carte overview à partir du même `assetId`. Réalise UJ-1.

#### FR-1 : Registre d'actifs configurable

Dokk peut déclarer un nouvel actif dans la configuration sans créer un nouveau `*Service` dupliqué.

**Consequences (testable) :**
- Entrée YAML avec `id`, `provider` (`fmp` | `scrape`), `symbol` ou `scrape-url` + `scrape-parser`, `type`, `currency`, `market-hours`, `sync-offset-minutes`.
- `AssetSyncJob` itère le registre et planifie la sync selon le provider de chaque actif.
- ISIN documenté en commentaire YAML ; pas de résolution ISIN runtime v1.

#### FR-2 : Endpoint REST par actif

Le front récupère les données d'un actif via `GET /api/dashboard/{assetId}` avec le même contrat de base qu'InveB (prix, mcap si dispo, Δ24h, volume, timestamps, séries `history*` et `live*`, blocs `dividends` et `fundamentals` depuis config).

**Consequences (testable) :**
- Les 6 nouveaux actifs + InveB répondent sur leur `assetId` dédié.
- Réponse JSON avec `lastRefresh` epoch ms et `priceSource` (`fmp` | `scrape` | `cache`).
- Erreur provider → DTO d'erreur + cache stale si disponible (pattern InveB).

#### FR-3 : Persistance historique unifiée

Chaque actif écrit dans `AssetDaily` (intraday, heures marché) et `AssetSnapshot` (clôture) avec sa clé `symbol` normalisée, quelle que soit la source prix.

**Consequences (testable) :**
- Rétention inchangée : 7 jours daily, 365 jours snapshot.
- Graphiques annuel (365 pts) et intraday (≤ 144 pts) fonctionnent pour chaque actif.

**Feature-specific NFRs :**
- Sync prix max 1 appel FMP `/profile` ou 1 scrape HTTP par actif par cycle T0.
- Écriture `AssetDaily` uniquement pendant les heures de marché configurées.

---

### 4.2 Couche métier — dividendes et fondamentaux (config)

**Description :** Tous les actifs actions/ETF/REIT exposent dividendes et fondamentaux depuis des fichiers YAML versionnés (ou section `app.assets[].dividends` / `fundamentals`). Le backend calcule le rendement % et les CAGR sur snapshots quand applicable. Réalise UJ-1, UJ-4.

#### FR-4 : Dividendes en configuration + rendement calculé

Le système expose l'historique dividendes (config), le dividende forward courant (config), le rendement estimé calculé et la croissance moyenne si fournie en config.

**Consequences (testable) :**
- DTO contient `dividendHistory[]`, `forwardDividend`, `estimatedYield`, `avgDividendGrowth10Y` (optionnel).
- `estimatedYield` = `forwardDividend` / `currentPrice` × 100 — **jamais** de constante hardcodée en front.
- Aucun appel API externe pour dividendes en v1.
- Migration InveB : suppression des constantes `inveb-dividend-card`.

#### FR-5 : Fondamentaux en configuration + alerte rapport trimestriel

Le système expose ratios, NAV, TER, holdings et métriques métier depuis la config ; signale les actifs dont `fundamentals.updatedAt` dépasse le seuil (défaut 90 jours).

**Consequences (testable) :**
- DTO `fundamentals` alimenté depuis YAML ; champ `updatedAt` ISO date.
- Bandeau ou section dashboard « À mettre à jour » listant les `assetId` en alerte.
- `topHoldings`, dry powder, cash inflow : champs config avec label « Source : rapport ».
- Migration InveB : suppression de `INVEB_FUNDAMENTALS_METRICS` hardcodé.
- Aucun appel FMP dividends/ratios/etf en v1.

#### FR-6 : Workflow MAJ config assistée LLM (hors runtime)

Dokk peut mettre à jour le YAML fondamentaux/dividendes via son workflow habituel (LLM + rapport PDF) quand l'alerte FR-5 se déclenche — pas d'intégration LLM dans l'app.

**Consequences (testable) :**
- Documentation addendum décrit le format YAML et un checklist trimestriel.
- Après MAJ manuelle du fichier et redéploiement/reload config, l'alerte disparaît pour l'actif concerné.

---

### 4.4 Actifs actions — spécifications par titre

#### 4.4.1 BlackRock World Mining (priorité 1)

| Attribut | Valeur |
|----------|--------|
| Type | Action (investment trust LSE) |
| Prix | FMP — `BRWM.L` |
| Dividendes / fondamentaux | Config YAML |
| Devise | GBP |
| `assetId` | `brwm` |

#### FR-8 : Page BlackRock World Mining

Dokk consulte BRWM avec graphiques InveB, carte dividende (config + yield calculé) et carte valorisation trust (NAV/discount config).

**Consequences (testable) :**
- Route `/brwm`, fuseau `Europe/London` 08:00–16:35.
- Prix sync FMP T0 15 min.

---

#### 4.4.2 Realty Income (priorité 2)

| Attribut | Valeur |
|----------|--------|
| Type | REIT |
| Prix | FMP — `O` |
| Dividendes / fondamentaux | Config YAML |
| Devise | USD |
| `assetId` | `o` |

#### FR-9 : Page Realty Income

Dokk consulte Realty Income avec focus dividende mensuel (historique config) et rendement calculé.

**Consequences (testable) :**
- Route `/o`, fuseau `America/New_York` 09:30–16:00.

---

#### 4.4.3 3i Group (priorité 3)

| Attribut | Valeur |
|----------|--------|
| Type | Action (LSE) |
| Prix | FMP — `III.L` |
| Dividendes / fondamentaux | Config YAML |
| Devise | GBP |
| `assetId` | `iii` |

#### FR-10 : Page 3i Group

Dokk consulte 3i avec pattern InveB et carte valorisation NAV/discount (config).

**Consequences (testable) :**
- Route `/iii`, fuseau `Europe/London`.

---

### 4.5 Actifs ETF

**Description :** Activer la section ETF (retirer « COMING SOON »). Prix live + graphiques InveB ; métriques ETF depuis config.

#### 4.5.1 iShares Global Infrastructure UCITS ETF (priorité 4)

| Attribut | Valeur |
|----------|--------|
| Type | ETF |
| ISIN | IE00B1FZS467 |
| Prix | FMP — `INFR.L` |
| Dividendes / fondamentaux | Config YAML |
| Devise | USD |
| `assetId` | `infr` |

#### FR-13 : Page Global Infrastructure ETF

Dokk consulte INFR avec donut sectoriel (poids secteurs config) et graphiques InveB.

**Consequences (testable) :**
- Route `/infr`, fuseau `Europe/London`.
- Composant partagé `EtfSectorChart` alimenté par DTO config.

---

### 4.6 Investor AB — migration vers le modèle 3 couches

**Description :** InveB conserve FMP pour le prix ; dividendes et fondamentaux migrent vers config + yield calculé + alerte trimestrielle. Réalise UJ-4.

#### FR-14 : Investor AB aligné sur FR-4 et FR-5

InveB utilise le même contrat DTO que les autres actions : prix FMP live, dividendes/fondamentaux config.

**Consequences (testable) :**
- `InveBService` délègue au service générique ou partage le registre.
- Suppression des constantes front existantes.
- `INVE-B.ST` validé FMP profile au spike.

---

### 4.7 Overview & navigation

#### FR-15 : Cartes overview pour tous les actifs

Dokk voit une carte par actif sur `/` avec prix live, Δ24h et lien vers la page détail.

**Consequences (testable) :**
- Section ETF active (plus `opacity-40` / « COMING SOON »).
- 6 cartes hors HYPE + HYPE ; refresh front pattern existant (~3 min).
- Devise affichée par carte.

#### FR-16 : Navbar et routes lazy

La navigation inclut tous les actifs sans régression HYPE/InveB.

**Consequences (testable) :**
- Routes lazy `features/stocks/{asset}/` et `features/etf/{asset}/`.
- 404 inchangé → redirect `/`.

---

### 4.8 Maîtrise des appels externes

**Description :** Contrainte quota FMP réduite (5 actifs prix seul), charge VPS, politesse scrape. Réalise contrainte produit transversale.

#### FR-17 : Sync prix FMP par provider

Le système synchronise les prix FMP toutes les 15 minutes (5 actifs, décalés, heures marché).

**Consequences (testable) :**
- HYPE inchangé (CoinGecko + Hyperliquid, 10 min).
- FMP : ~5 × 26 ≈ **130 appels/jour** — marge confortable sur quota 250.
- Aucun appel FMP sur `GET /api/dashboard/*`.

#### FR-18 : Observabilité des appels

Dokk peut vérifier en logs les compteurs FMP et scrape séparés.

**Consequences (testable) :**
- Log INFO : `FMP calls today: N`, `Scrape calls today: M`.
- Warning FMP si N > 200.
- Warning scrape si taux d'échec parse > 20 % sur 1 h.

---

## 5. Non-Goals (Explicit)

- Nouvelle crypto ou extension pattern HYPE.
- Authentification / multi-utilisateur.
- Alertes prix, notifications push, export CSV.
- Conversion forex temps réel entre devises du portefeuille.
- **Fondamentaux ou dividendes live via API** (FMP payant, Finnhub, EODHD).
- Lib **yfinance** (Python) ou scrape HTTP Yahoo/SIX.
- **Finnhub** comme provider (spike : tier free insuffisant pour international/ETF).
- Scrape HTML émetteurs ETF.
- Agrégateurs commerciaux en production.
- Intégration LLM in-app pour MAJ config.
- Flyway / migrations formelles (rester sur `ddl-auto: update`).
- Application mobile native.

---

## 6. MVP Scope

### 6.1 In Scope

1. Registre multi-provider + service générique (FR-1 à FR-3).
2. Couche config dividendes + fondamentaux + alerte trimestrielle (FR-4 à FR-6).
3. Quatre nouveaux actifs + migration InveB (FR-8 à FR-10, FR-13, FR-14).
4. Overview + routes (FR-15, FR-16).
5. Sync et observabilité (FR-17, FR-18).
6. Composants UI partagés : `AssetMainCard`, `PriceChart`, `DailyChart`, cartes génériques `DividendCard`, `FundamentalsCard`, `EtfMetricsCard`.

### 6.2 Out of Scope for MVP

| Item | Raison |
|------|--------|
| Endpoint `/overview` agrégé | Optimisation v2 |
| FMP dividends/ratios/etf endpoints | 402/401 au spike — remplacé par config |
| Parse dividende depuis source externe | Optionnel v2 ; config suffit v1 |
| Tests E2E Playwright | Hors demande |
| i18n EN | FR suffisant usage perso |

---

## 7. Success Metrics

**Primary**
- **SM-1** : Dokk consulte le dashboard ≥ 3×/semaine pendant 4 semaines après déploiement. Valide FR-15.
- **SM-2** : Ajout d'un 9ᵉ actif FMP en < 2 h (config + route + carte). Valide FR-1.

**Secondary**
- **SM-3** : Appels FMP < 150/jour en régime stable (5 actifs, prix seul). Valide FR-17, FR-18.
- **SM-4** : Zéro constante front pour dividendes/fondamentaux InveB. Valide FR-4, FR-5, FR-14.
- **SM-5** : Taux succès scrape CHDIV/QQQE > 95 % en heures SIX sur 1 semaine. Valide FR-7.

**Counter-metrics (ne pas optimiser)**
- **SM-C1** : Nombre total de métriques affichées — la clarté prime sur la densité.

---

## 8. Open Questions

1. ~~URL UBS~~ — résolu : Yahoo chart (spike `spike-scrape-ubs-report-2026-06-17.md`).
2. Reload config YAML sans redéploiement : `@ConfigurationProperties` + actuator refresh ou restart Docker acceptable ?
3. Seuil alerte fondamentaux : 90 jours fixe ou par type d'actif (ETF vs action) ?
4. Ordre de livraison : cadre générique d'abord, puis actifs par priorité ?

---

## 9. Assumptions Index

- **A-1** (§4.8) — 5 actifs FMP × sync 15 min ≈ 130 req/j, sous quota 250.
- **A-2** (§4.2) — MAJ config fondamentaux ~trimestrielle manuelle acceptable pour usage perso.
- **A-3** (§4.8) — Seuil warning compteur FMP à 200/jour.
- **A-4** (§2.2) — Pas de conversion devises cross-portefeuille v1.
- **A-5** (§1) — Spike FMP : `INFR.L` et 4 actions (InveB, BRWM, O, III) validés.

---

## Annexe — Catalogue actifs (référence implémentation)

| Prio | Nom | Type | Prix (provider) | Div / fond. | Devise |
|------|-----|------|-----------------|-------------|--------|
| — | Investor AB | Action | FMP `INVE-B.ST` | Config | SEK |
| 1 | BlackRock World Mining | Trust | FMP `BRWM.L` | Config | GBP |
| 2 | Realty Income | REIT | FMP `O` | Config | USD |
| 3 | 3i Group | Action | FMP `III.L` | Config | GBP |
| 4 | iShares Global Infra ETF | ETF | FMP `INFR.L` | Config | USD |

*Détail technique : `addendum.md` · Décisions : `.decision-log.md`*
