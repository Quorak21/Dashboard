# Addendum — Détails techniques (hors PRD)

## Modèle données 3 couches

```
┌─────────────────────────────────────────────────────────────┐
│  COUCHE PRIX (live)                                         │
│  HYPE → CoinGecko + Hyperliquid (inchangé)                  │
│  5 actifs → FMP /stable/profile (T0, 15 min)                │
│  CHDIV, QQQE → Yahoo chart API (T0, 10 min, heures SIX)      │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│  COUCHE DIVIDENDES (config)                                 │
│  Historique + forwardDividend en YAML                       │
│  estimatedYield = forwardDividend / currentPrice × 100      │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│  COUCHE FONDAMENTAUX (config + alerte)                      │
│  Ratios, NAV, TER, holdings, sector weights en YAML         │
│  updatedAt → alerte dashboard si > 90 jours                 │
│  MAJ manuelle ~trimestrielle via workflow LLM (hors app)    │
└─────────────────────────────────────────────────────────────┘
```

## Résultats spike providers (2026-06-17)

| Actif | FMP `/stable/profile` | Finnhub quote |
|-------|----------------------|---------------|
| INVE-B.ST, BRWM.L, O, III.L, INFR.L | ✅ prix OK | ❌ 403 international |
| CHDIV.SW, QQQE.SW | ❌ vide | ❌ 403 |
| FMP `/stable/dividends`, `/ratios`, `/etf/*` | ❌ 402/401 | — |

**Conséquence :** pas de second provider payant ; scrape ciblé pour 2 ETF SIX.

---

## Architecture cible (brownfield)

### Backend — providers

```
providers/
  fmp/
    FMPClient              # profile uniquement v1
  scrape/
    ScrapePriceProvider
    YahooSixChartParser    # regularMarketPrice depuis chart API
    ScrapePriceCache

features/
  assets/
    generic/               # AssetRegistry, GenericAssetService, AssetDto
    investorab/            # refactor → registre + config
  config/
    AssetDividendsConfig     # YAML dividends par assetId
    AssetFundamentalsConfig  # YAML fundamentals + updatedAt
    QuarterlyReportAlertService
```

### Registre actifs (`assets-registry.yml`)

```yaml
app:
  assets:
    - id: brwm
      provider: fmp
      symbol: BRWM.L
      type: STOCK
      currency: GBP
      market-hours: Europe/London
      sync-offset-minutes: 0
    - id: chdiv
      provider: scrape
      symbol: CHDIV.SW
      isin: CH1244681594
      scrape-parser: yahoo-six-chart
      type: ETF
      currency: CHF
      market-hours: Europe/Zurich
      sync-interval-minutes: 10
    - id: qqqe
      provider: scrape
      symbol: QQQE.SW
      isin: IE0003RQ9F90
      scrape-parser: yahoo-six-chart
      type: ETF
      currency: USD
      market-hours: Europe/Zurich
      sync-interval-minutes: 10
    - id: infr
      provider: fmp
      symbol: INFR.L
      type: ETF
      currency: USD
      market-hours: Europe/London
```

### Config dividendes (exemple `config/dividends/inveb.yml`)

```yaml
assetId: inveb
forwardDividend: 6.00
forwardDividendCurrency: SEK
frequency: annual
history:
  - year: 2024
    amount: 6.00
    currency: SEK
  - year: 2023
    amount: 5.50
    currency: SEK
avgDividendGrowth10Y: 8.2  # optionnel, saisi manuellement
```

### Config fondamentaux (exemple)

```yaml
assetId: brwm
updatedAt: "2026-03-15"
source: "Rapport semestriel 2025"
metrics:
  navPerShare: 612.0
  discountToNav: -8.5
  trailingPE: 12.4
topHoldings: []  # optionnel
```

### Calcul rendement (backend)

```java
// estimatedYield = forwardDividend / currentPrice * 100
// currentPrice depuis cache FMP ou scrape
// forwardDividend depuis AssetDividendsConfig
```

---

## Stratégie sync et quotas

| Provider | Actifs | Fréquence | Est. appels/jour |
|----------|--------|-----------|------------------|
| FMP profile | 5 (inveb, brwm, o, iii, infr) | 15 min, heures marché | ~130 |
| Scrape Yahoo chart | 2 (chdiv, qqqe) | 10 min, SIX 09:00–17:30 | ~96 |
| CoinGecko/HYPE | 1 | 10 min | budget séparé |

**Garde-fous scrape :**
- User-Agent : `DashboardBot/1.0 (+https://dashboard.dokkcorp.ch)`
- Timeout 15 s, retry max 2
- Pas de scrape nuit/week-end
- Fixture JSON en tests unitaires (reponses spike)
- Fallback cache + `priceSource: cache`

**Garde-fous FMP** (inchangés) :
- Cache `AtomicReference` par actif
- Pas d'appel sur requête HTTP front
- Décalage `sync-offset-minutes` anti-burst

---

## Sources scrape (validées spike 2026-06-17)

| Actif | Source retenue | Champ | Exemple spike |
|-------|----------------|-------|---------------|
| CHDIV.SW | Yahoo chart API | `meta.regularMarketPrice` | 13.868 CHF |
| QQQE.SW | Yahoo chart API | `meta.regularMarketPrice` | 34.995 USD |

```
GET https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=1d&range=1d
```

**Rejetées au spike :**
- **UBS émetteur** — coquille React fundgate ; 403 WAF ; 0 prix en HTTP simple
- **justETF** — quotes via Wicket AJAX, pas de prix dans HTML initial
- **Deutsche Börse** — OK pour QQQE mais listing Frankfurt EUR, pas SIX USD
- **SIX API** — 401 auth requise

**Note :** pas de lib `yfinance` — requete HTTP JSON via `RestClient` uniquement (D-21).

Rapport : `research/spike-scrape-ubs-report-2026-06-17.md`

---

## Endpoints API

| Route | Description |
|-------|-------------|
| `GET /api/dashboard/{assetId}` | Prix live + séries BD + blocs dividends/fundamentals config |
| `GET /api/dashboard/alerts/quarterly` | Liste actifs en alerte fondamentaux *(optionnel v1 — peut être inline overview DTO)* |

---

## Migration Investor AB

| Donnée | v0 (actuel) | v1 (ce PRD) |
|--------|-------------|-------------|
| Prix | FMP live | FMP live (inchangé) |
| Dividend history | Constantes front | `config/dividends/inveb.yml` |
| Rendement | Constante 6 SEK hardcodée | Calculé `forwardDividend / price` |
| P/E, levier | Constantes front | `config/fundamentals/inveb.yml` |
| 5Y CAGR | Constante | Calcul snapshots BD (inchangé si données suffisantes) |
| Top holdings | Constante | Config + label « Source : rapport » |
| Alerte fraîcheur | — | `updatedAt` + seuil 90 j |

---

## Alerte rapport trimestriel (UI)

- Bandeau discret sous navbar ou carte dédiée overview
- Texte : « Fondamentaux à vérifier : InveB, BRWM (dernière MAJ > 90 j) »
- Clic → lien vers doc checklist MAJ config (README interne)
- Pas de date earnings API — heuristique `updatedAt` + calendrier trimestriel fixe par marché `[ASSUMPTION]`

---

## Alternatives rejetées

| Option | Raison rejet |
|--------|--------------|
| Finnhub free | Spike : quote OK seulement `O` ; international/ETF ISIN → 403 |
| FMP dividends/ratios/etf | 402/401 — hors plan Starter |
| yfinance (lib Python) | Écarté ; HTTP JSON Yahoo chart accepté (D-21) |
| UBS HTML scrape | Spike : fundgate React, non viable |
| FMP Premium $59/mo | Coût évité ; scrape + config suffisent usage perso |
| 1 service Java par actif | Dette copier-coller |
| Scrape généralisé | Fragilité ; limité à 2 ETF non couverts FMP |
| Fondamentaux live API | Complexité/quota incompatible hobby solo |
