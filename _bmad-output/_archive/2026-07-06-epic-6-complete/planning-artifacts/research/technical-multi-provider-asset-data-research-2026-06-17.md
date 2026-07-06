---
stepsCompleted: [1, 2, 3, 4, 5, 6]
inputDocuments:
  - docs/project-context.md
  - _bmad-output/planning-artifacts/prds/prd-Dashboard-2026-06-17/prd.md
  - _bmad-output/planning-artifacts/prds/prd-Dashboard-2026-06-17/addendum.md
workflowType: research
lastStep: 6
research_type: technical
research_topic: Sources de données multi-actifs (FMP et alternatives) pour Dashboard
research_goals: >
  Valider go/no-go par actif et par type de donnée ; comparer FMP vs alternatives
  (Finnhub, EODHD, Alpha Vantage, Twelve Data) ; recommander architecture provider
  brownfield avec contrainte quota VPS ; alimenter mise à jour PRD et architecture.
user_name: Dokk
date: 2026-06-17
web_research_enabled: true
source_verification: true
status: final
---

# Recherche technique : sources de données multi-actifs pour Dashboard

**Date :** 2026-06-17 · **Auteur :** Dokk · **Type :** Technique · **Statut :** Final

---

## Research Overview

Cette recherche répond au bloquant identifié en party mode (FR-0) : le PRD suppose que **FMP seul** couvre les 7 actifs FMP avec ~210 appels/jour sur plan Starter. Les données publiques montrent une **ambiguïté sur la couverture internationale par tier FMP** (UK/Canada explicitement sur Premium $59/mo selon [TradingDataCompare](https://tradingdatacompare.com/providers/financial-modeling-prep/)), alors que **InveB fonctionne déjà en prod** via `INVE-B.ST` — donc ton plan actuel couvre au minimum Stockholm.

**Conclusion principale :** ne pas verrouiller l’architecture sur FMP seul. Adopter un **modèle multi-provider** avec **Finnhub en candidat principal** pour les nouveaux actifs (quota, ETF par ISIN, secteurs), **FMP conservé** pour InveB tant que la parité n’est pas prouvée, et **EODHD en option payante** pour résolution ISIN + fondamentaux profonds si besoin.

Le détail, les matrices go/no-go et la stratégie de sync sont ci-dessous.

---

## Executive Summary

Le Dashboard brownfield dispose déjà de trois familles de providers (`CoinGecko`, `Hyperliquid`/RPC, `FMP`). Pour passer de 2 à 9 actifs sans exploser les quotas ni la dette code, la meilleure trajectoire est :

1. **Spike manuel 30 min** (curl) sur FMP + Finnhub pour les 7 symboles/ISIN — **bloquant avant dev**.
2. **Architecture cible :** registre d’actifs avec champ `provider` + `providerSymbol` / `isin` par actif.
3. **Recommandation provider :**
   - **HYPE** → inchangé (CoinGecko + Hyperliquid + RPC).
   - **Nouveaux actifs + ETF** → **Finnhub** (gratuit 60 req/min, ETF holdings/secteurs **par ISIN**).
   - **InveB** → **FMP** court terme (déjà intégré, prod OK) ; migration Finnhub après test de parité dividendes/métriques.
   - **EODHD** → optionnel (~€18–54/mo) si Finnhub/FMP manquent NAV trust ou fondamentaux UCITS.
4. **À éviter en prod :** Alpha Vantage (25 req/j), yfinance non officiel, Twelve Data EU temps réel ($229/mo).

**Budget quota révisé (7 actifs FMP-like, sync 15 min heures marché) :**

| Provider | Limite typique | Marge pour ~200–350 appels/jour |
|----------|----------------|----------------------------------|
| FMP Free | 250/jour | Juste si tout sur FMP |
| FMP Starter | 300/min | Large (si plan payant) |
| Finnhub Free | 60/min | **Très large** (~8 600/jour théorique) |
| EODHD Free | 20/jour | Insuffisant sauf bootstrap ISIN |
| Alpha Vantage Free | 25/jour | **Non viable** |

---

## Table des matières

1. [Contexte et objectifs](#1-contexte-et-objectifs)
2. [Paysage des providers](#2-paysage-des-providers)
3. [Matrice comparative](#3-matrice-comparative)
4. [Couverture par actif](#4-couverture-par-actif)
5. [Architecture recommandée](#5-architecture-recommandée)
6. [Stratégie sync et quotas](#6-stratégie-sync-et-quotas)
7. [Risques et mitigations](#7-risques-et-mitigations)
8. [Décisions go/no-go et prochaines étapes](#8-décisions-gono-go-et-prochaines-étapes)
9. [Sources](#9-sources)

---

## 1. Contexte et objectifs

### 1.1 Besoin produit

| # | Actif | Type | Identifiant prioritaire |
|---|-------|------|-------------------------|
| — | Investor AB | Action | `INVE-B.ST` (FMP en prod) |
| 1 | BlackRock World Mining | Trust LSE | `BRWM.L` |
| 2 | Realty Income | REIT US | `O` |
| 3 | 3i Group | Action LSE | `III.L` |
| 4 | UBS Swiss Dividend ETF | ETF | ISIN `CH1244681594` / `CHDIV` |
| 5 | UBS Nasdaq 100 ETF | ETF | ISIN `IE0003RQ9F90` / `QQQE` |
| 6 | iShares Global Infra ETF | ETF | ISIN `IE00B1FZS467` / `INFR` |

### 1.2 Données requises par tier (PRD)

| Tier | Données | Fréquence cible |
|------|---------|-----------------|
| T0 | Prix, Δ24h, volume, mcap | 15 min, heures marché |
| T1 | Dividendes, calendrier | 1×/jour |
| T2 | Ratios, P/E, métriques clés | 1×/jour |
| T3 | ETF holdings, sector weights | 1×/semaine |

### 1.3 Contraintes techniques existantes

- Spring Boot : `FMPClient`, `ExternalCallExecutor`, pattern `features/*/providers/`
- Pas d’appel provider sur `GET` HTTP — cache + job
- VPS : RAM limitée, pas de burst inutile
- HYPE : budget API séparé (CoinGecko + Hyperliquid)

---

## 2. Paysage des providers

### 2.1 Financial Modeling Prep (FMP) — *déjà intégré*

**Forces**
- Déjà en prod pour `INVE-B.ST` via `/stable/profile`
- Endpoints fondamentaux, dividendes, ETF documentés
- 250 appels/jour en free ; Starter ~$22/mo (300/min) ; Premium $59/mo ajoute **UK + Canada** explicitement ([pricing FMP](https://site.financialmodelingprep.com/pricing-plans))

**Faiblesses**
- Free tier orienté US dans la doc FAQ ; couverture internationale **selon tier ambiguë**
- Quota **journalier** en free (250/j) — serré pour 7 actifs × sync 15 min
- ETF UCITS SIX (`CHDIV.SW`, `QQQE.SW`) : **non vérifié** sans spike
- Pas de lookup ISIN natif aussi ergonomique que Finnhub pour ETF

**Verdict Dashboard :** **Garder** pour InveB ; **ne pas étendre seul** aux 6 nouveaux actifs sans spike.

---

### 2.2 Finnhub — *recommandé pour nouveaux actifs*

**Forces**
- **60 appels/minute** en free ([pricing Finnhub](https://finnhub.io/pricing)) — bien plus confortable que 250/jour FMP
- Couverture **globale** : LSE (15 min delay), autres marchés internationaux en **EOD**
- **ETF par ISIN** : `/etf/profile`, `/etf/holdings`, `/etf/sector-exposure` avec param `isin` ([docs Finnhub Python](https://github.com/Finnhub-Stock-API/finnhub-python))
- Dividendes 30 ans, key metrics, symbol lookup par ISIN
- Symbologie documentée : `Ticker.ExchangeCode` (ex. exchange Stockholm = `ST` selon doc stock-symbols)

**Faiblesses**
- Fondamentaux détaillés : tableau pricing indique **US** en free, **Global** en payant pour certaines facettes
- LSE : **15 min de délai** (acceptable pour dashboard perso)
- Stockholm / SIX : probablement **EOD** hors US — OK pour graphiques annuels + snapshot 15 min si quote disponible
- Liste ETF supportés : vérifier que tes ISIN UCITS y figurent ([liste ETF Finnhub](https://finnhub.io/docs/api/etfs-holdings))

**Verdict Dashboard :** **Candidat #1** pour BRWM, O, III, et les 3 ETF (via ISIN).

---

### 2.3 EOD Historical Data (EODHD)

**Forces**
- **Recherche par ISIN** native ([Search API](https://eodhd.com/financial-apis/search-api-for-stocks-etfs-mutual-funds))
- 150k+ tickers, 70+ bourses, fondamentaux ETF riches
- Format `SYMBOL.EXCHANGE` (ex. `BRWM.LSE`, `CHDIV.SW`)
- Plan EOD All World ~**€17.99/mo** ; Fundamentals ~€53.99/mo ([pricing](https://eodhd.com/pricing-special-10))

**Faiblesses**
- Free : **20 appels/jour** seulement — inutilisable en sync continue
- Coût récurrent si utilisé comme provider principal
- Un provider de plus à maintenir

**Verdict Dashboard :** **Provider secondaire / bootstrap** — résolution ISIN→ticker une fois, fondamentaux batch 1×/jour si Finnhub/FMP insuffisants. Pas en T0 prix.

---

### 2.4 Alpha Vantage

- Free : **25 requêtes/jour**, 5/min ([support AV](https://www.alphavantage.co/support/))
- Couverture globale annoncée mais volume **incompatible** avec 7 actifs en sync 15 min
- Premium dès $49.99/mo pour usage sérieux

**Verdict :** ❌ **Écarté** pour ce projet.

---

### 2.5 Twelve Data

- Free : 800 crédits/jour — intéressant en volume
- Données EU temps réel / Cboe : plans **$229/mo+** ([Twelve Data pricing](https://tradingdatacompare.com/providers/twelve-data/))
- ETF holdings souvent sur plans business

**Verdict :** ❌ **Surdimensionné** pour usage perso ; possible en repli EOD uniquement si free suffit.

---

### 2.6 Autres pistes (non recommandées v1)

| Provider | Pourquoi pas v1 |
|----------|-----------------|
| **yfinance / Yahoo non officiel** | Pas de SLA, risque ToS, fragile en prod Docker |
| **Polygon / Massive** | US-centric, peu de valeur pour UCITS SIX |
| **OpenFIGI** | Mapping ISIN seulement, pas de prix |
| **CoinGecko** | Déjà réservé HYPE ; pas d’actions |
| **JustETF / scraping** | Fragile, hors pattern `RestClient` actuel |

---

## 3. Matrice comparative

| Critère | FMP (actuel) | Finnhub | EODHD | Alpha Vantage |
|---------|--------------|---------|-------|---------------|
| **Coût perso** | $0–22/mo (?) | $0 | €18–54/mo utile | $0 (inutilisable) |
| **Quota free** | 250/**jour** | 60/**min** | 20/**jour** | 25/**jour** |
| **US (O)** | ✅ | ✅ | ✅ | ✅ |
| **LSE (BRWM, III, INFR)** | ⚠️ tier Premium? | ✅ EOD / LSE 15m | ✅ payant | ⚠️ |
| **Stockholm (InveB)** | ✅ **prod** | ⚠️ à spike | ✅ | ⚠️ |
| **ETF par ISIN** | ⚠️ symbole | ✅ **natif** | ✅ search | ⚠️ |
| **ETF sector weights** | ✅ endpoints ETF | ✅ `/etf/sector-exposure` | ✅ fundamentals | ❌ |
| **Dividendes** | ✅ | ✅ | ✅ payant | ✅ limité |
| **Trust NAV/discount** | ⚠️ inconnu | ⚠️ inconnu | ⚠️ partiel | ❌ |
| **Intégration existante** | ✅ Java | ❌ à ajouter | ❌ à ajouter | ❌ |
| **Effort d’ajout** | Faible | Moyen (1 client) | Moyen | Faible mais inutile |

---

## 4. Couverture par actif

> **Confiance :** 🟢 confirmé par doc ou prod · 🟡 probable · 🔴 spike requis

| Actif | Symbole / ISIN | Provider recommandé | T0 Prix | T1 Div | T2 Ratios | T3 ETF | Confiance |
|-------|----------------|---------------------|---------|--------|-----------|--------|-----------|
| **Investor AB** | `INVE-B.ST` | **FMP** (garder) | 🟢 FMP prod | 🟡 FMP `/dividends` | 🟡 FMP ratios | N/A | 🟢 |
| **BlackRock World Mining** | `BRWM.L` | **Finnhub** `BRWM.L` | 🟡 | 🟡 | 🟡 | N/A | 🔴 spike |
| **Realty Income** | `O` | **Finnhub** ou FMP | 🟢 | 🟢 | 🟡 P/FFO | N/A | 🟡 |
| **3i Group** | `III.L` | **Finnhub** `III.L` | 🟡 | 🟡 | 🟡 | N/A | 🔴 spike |
| **UBS Swiss Dividend** | `CH1244681594` | **Finnhub** `isin=` | 🟡 | 🟡 | 🟡 TER | 🟢 holdings/sector | 🔴 spike ISIN |
| **Nasdaq 100 ETF** | `IE0003RQ9F90` | **Finnhub** `isin=` | 🟡 | 🟡 | 🟡 | 🟢 top 10 | 🔴 spike ISIN |
| **Global Infra ETF** | `IE00B1FZS467` | **Finnhub** `isin=` | 🟡 | 🟡 | 🟡 | 🟢 **sector donut** | 🔴 spike ISIN |

### 4.1 Notes par actif

**BRWM / III (investment trusts)**  
Le PRD cible NAV / discount. Aucun provider grand public ne garantit un NAV trust fiable sur API cheap. **Recommandation :** exposer `nav: null`, `navSource: "UNAVAILABLE"` si spike négatif ; saisie manuelle YAML en repli (comme holdings InveB).

**Realty Income (O)**  
Meilleur candidat US : FMP et Finnhub devraient couvrir prix + dividendes mensuels. P/FFO : vérifier champ disponible (souvent `priceToBook` / métriques REIT imparfaites).

**ETF UCITS (CHDIV, QQQE, INFR)**  
**Avantage décisif Finnhub :** appels directs par ISIN sans deviner `CHDIV.SW` vs `QQQE.L`. EODHD en backup pour résoudre `SYMBOL.EXCHANGE` si Finnhub ne liste pas l’ISIN.

---

## 5. Architecture recommandée

### 5.1 Pattern brownfield (évolution, pas révolution)

```
providers/
  coingecko/     # HYPE — inchangé
  hyperliquid/   # HYPE — inchangé
  blockchain/    # HYPE — inchangé
  fmp/           # InveB + fallback US/LSE
  finnhub/       # NOUVEAU — actifs + ETF par défaut

features/
  registry/
    AssetRegistryConfig.yaml   # id, type, provider, symbol, isin, timezone, tiers
  generic/
    AssetDataService           # route vers le bon client
```

**Registre enrichi (extrait) :**

```yaml
assets:
  - id: inveb
    provider: fmp
    symbol: INVE-B.ST
    type: STOCK
    currency: SEK
    marketHours: Europe/Stockholm

  - id: chdiv
    provider: finnhub
    isin: CH1244681594
    symbol: CHDIV.SW          # fallback si quote par symbole
    type: ETF
    currency: CHF
    marketHours: Europe/Zurich

  - id: infr
    provider: finnhub
    isin: IE00B1FZS467
    symbol: INFR.L
    type: ETF
    currency: USD
    marketHours: Europe/London
```

### 5.2 Stratégie de migration

| Phase | Action |
|-------|--------|
| **R0** | Spike curl FMP + Finnhub (7 actifs) — tableau résultats |
| **R1** | `FinnhubClient` + tests ; registre YAML ; `AssetDataService` |
| **R2** | InveB live dividendes via **FMP** (FR-4/5 PRD) |
| **R3** | 1 actif pilote Finnhub (suggéré : **O** ou **INFR** par ISIN) |
| **R4** | Basculer les 5 restants = config uniquement |
| **R5** | (Optionnel) Parité InveB sur Finnhub → retirer FMP si un seul key à gérer |

### 5.3 Pourquoi pas « Finnhub only » immédiatement ?

- InveB **marche** sur FMP — migration = risque de régression (party mode Winston/Amelia).
- Deux providers temporaires = acceptable : **2 clés API**, pattern `providers/` déjà établi.
- Objectif fin : **1 provider actions/ETF** une fois parité validée.

---

## 6. Stratégie sync et quotas

### 6.1 Scénario A — tout FMP (PRD initial)

| Tier | Appels/j/actif/jour | × 7 actifs | Total |
|------|---------------------|------------|-------|
| T0 (15 min, ~6.5h marché) | ~26 | 182 | |
| T1 + T2 | 2 | 14 | |
| T3 (amorti) | ~0.3 | 2 | |
| **Total** | | | **~198/jour** |

✅ Sous 250/j **si** plan free et **si** tous les symboles répondent.  
⚠️ Marge quasi nulle ; retries = dépassement.  
⚠️ Si Premium requis pour LSE ($59/mo) — hypothèse PRD **invalidée** côté coût.

### 6.2 Scénario B — Finnhub principal (recommandé)

| Tier | Appels/j/actif | × 7 | Notes |
|------|----------------|-----|-------|
| T0 | ~26 | 182 | 60/min → ~3 min de burst max |
| T1+T2 | 2–3 | 14–21 | quote + metrics + dividend |
| T3 hebdo | 2 | 2/j amorti | holdings + sector |

**~200 appels/jour** mais limite **minute** — largement OK.  
Marge pour HYPE (CoinGecko) indépendante.

### 6.3 Scénario C — hybride FMP (InveB) + Finnhub (6 actifs)

- FMP : ~30 appels/jour (InveB seul, tiers réduits)
- Finnhub : ~170 appels/jour (6 actifs)
- **Meilleur équilibre** coût / risque / couverture ETF ISIN

### 6.4 Garde-fous (tous scénarios)

- Compteur par provider + log daily (FR-15)
- Circuit breaker > 80 % quota FMP ; Finnhub > 50 req/min soutenu
- **Jamais** d’appel provider sur GET front
- Schedulers **séparés** par tier (T0 ≠ T1/T2 ≠ T3)

---

## 7. Risques et mitigations

| Risque | Impact | Mitigation |
|--------|--------|------------|
| ISIN UCITS absent chez Finnhub | ETF bloqués | EODHD search 1× ; repli ticker `.L` / `.SW` en YAML |
| NAV trust indisponible | Métrique BRWM/III vide | `null` + config manuelle ; pas de faux calcul |
| FMP free sans LSE | BRWM/III/Infr via FMP KO | Finnhub ou upgrade Premium |
| Deux providers = complexité | +1 client Java | Registre `provider` + interface commune |
| Données stale | Confiance utilisateur | Badge `lastRefresh` + toast si cache > 1h (Sally UJ-4) |
| Rate limit ban IP | Prod down | `ExternalCallExecutor` + offsets + pas de retry agressif |

---

## 8. Décisions go/no-go et prochaines étapes

### 8.1 Décisions proposées

| ID | Décision | Statut |
|----|----------|--------|
| D-R1 | Ajouter **Finnhub** comme provider actions/ETF pour nouveaux actifs | ✅ Recommandé |
| D-R2 | **Conserver FMP** pour InveB jusqu’à parité prouvée | ✅ Recommandé |
| D-R3 | **EODHD** optionnel payant — bootstrap ISIN uniquement si spike échoue | ⚠️ Conditionnel |
| D-R4 | Alpha Vantage, Twelve Data EU, yfinance | ❌ Écartés |
| D-R5 | Spike manuel **bloquant** avant `bmad-create-architecture` | ✅ Requis |
| D-R6 | Mettre à jour PRD : `provider` dans registre, FR-0 spike, fusion FR-6→11 | ✅ Recommandé |

### 8.2 Script spike (à exécuter par Dokk)

Remplacer `YOUR_FMP_KEY` / `YOUR_FINNHUB_KEY` :

```bash
# FMP — profile
curl "https://financialmodelingprep.com/stable/profile?symbol=BRWM.L&apikey=YOUR_FMP_KEY"
curl "https://financialmodelingprep.com/stable/profile?symbol=CHDIV.SW&apikey=YOUR_FMP_KEY"

# Finnhub — quote LSE
curl "https://finnhub.io/api/v1/quote?symbol=BRWM.L&token=YOUR_FINNHUB_KEY"

# Finnhub — ETF par ISIN
curl "https://finnhub.io/api/v1/etf/profile?isin=CH1244681594&token=YOUR_FINNHUB_KEY"
curl "https://finnhub.io/api/v1/etf/holdings?isin=IE00B1FZS467&token=YOUR_FINNHUB_KEY"
curl "https://finnhub.io/api/v1/etf/sector-exposure?isin=IE00B1FZS467&token=YOUR_FINNHUB_KEY"
```

Documenter : HTTP 200 ? champs prix/dividendes/holdings présents ?

### 8.3 Enchaînement BMad

1. ✅ Cette recherche (fait)
2. **`bmad-prd` Update** — intégrer D-R1 à R6, corriger assumptions A-1/A-5/A-6
3. **`bmad-create-architecture`** — `FinnhubClient`, registre multi-provider
4. **`bmad-create-epics-and-stories`** — spike R0 en story 0

---

## 9. Sources

| Source | URL | Usage |
|--------|-----|-------|
| FMP Pricing | https://site.financialmodelingprep.com/pricing-plans | Quotas, UK tier |
| FMP FAQs | https://site.financialmodelingprep.com/faqs | 250/j, fuseaux |
| Finnhub Pricing | https://finnhub.io/pricing | 60/min, LSE delay |
| Finnhub ETF Python SDK | https://github.com/Finnhub-Stock-API/finnhub-python | ISIN endpoints |
| EODHD Search API | https://eodhd.com/financial-apis/search-api-for-stocks-etfs-mutual-funds | ISIN lookup |
| EODHD Pricing | https://eodhd.com/pricing-special-10 | Plans payants |
| Alpha Vantage Support | https://www.alphavantage.co/support/ | 25/j limit |
| Twelve Data (compare) | https://tradingdatacompare.com/providers/twelve-data/ | EU pricing |
| FMP vs Finnhub | https://tradingdatacompare.com/compare/financial-modeling-prep-vs-finnhub/ | Comparaison |
| JustETF CHDIV | https://www.justetf.com/en/etf-profile.html?isin=CH1244681594 | Ticker CHDIV |
| Dashboard PRD | `_bmad-output/planning-artifacts/prds/prd-Dashboard-2026-06-17/prd.md` | Exigences |
| Dashboard code | `backend/.../FMPClient.java`, `InveBService.java` | Brownfield |

---

## Conclusion

FMP n’est **pas** le seul choix viable — et pour ton portefeuille **multi-bourses + ETF UCITS par ISIN**, **Finnhub en free tier** est probablement **meilleur** que d’étendre FMP seul. La trajectoire la plus sûre pour un dashboard perso sur VPS :

**CoinGecko/Hyperliquid (HYPE) + FMP (InveB) + Finnhub (tout le reste)**, avec spike 30 min avant d’écrire l’architecture.

*Recherche finalisée le 2026-06-17 — prête pour mise à jour PRD.*
