---
date: 2026-06-17
type: spike
topic: Scrape prix CHDIV.SW + QQQE.SW
status: final
verdict: ACCEPTED
decision: D-21 yahoo-six-chart
---

# Spike scrape — CHDIV & QQQE (SIX)

## Objectif

Valider qu'une source HTTP parseable fournit un **prix live SIX** pour les 2 ETF non couverts FMP, avant implémentation `ScrapePriceProvider`.

## Sources testées

| Source | CHDIV.SW | QQQE.SW | Notes |
|--------|----------|---------|-------|
| **UBS CH** (`ubs.com/ch/.../pd001.html`) | ❌ | ❌ | Coquille HTML + `fundgateHandler` React ; prix chargé côté client |
| **UBS NL** (`ubs.com/nl/.../pd001.html`) | ❌ 403 | ❌ 403 | WAF / bot depuis IP script ; WebFetch Cursor a obtenu QQQE avec table bid/ask |
| **Deutsche Börse** (`live.deutsche-boerse.com`, ISIN QQQE) | — | ⚠️ | `lastPrice` dans JSON embarqué (`boerse-frankfurt-state`) ; **listing Frankfurt EUR**, pas SIX USD |
| **Yahoo chart API** (`query1.finance.yahoo.com/v8/finance/chart/{symbol}`) | ✅ | ✅ | `regularMarketPrice`, devise correcte, exchange `Swiss` (EBS) |
| **justETF** | ⚠️ | ⚠️ | Page lourde ; quotes via Wicket AJAX — pas de prix dans HTML initial |
| **Swiss Fund Data** | ❌ | — | Fonds CHDIV non listé |
| **SIX API** (`api.six-group.com`) | ❌ 401 | ❌ 401 | Auth requise |

## Résultats prix (2026-06-17, session ouverte)

| Actif | Yahoo SIX | UBS NL (si accessible) | Deutsche Börse |
|-------|-----------|------------------------|----------------|
| CHDIV.SW | **13.868 CHF** | — | — |
| QQQE.SW | **34.995 USD** | bid 29.80 / ask 29.895 USD *(WebFetch uniquement)* | 30.125 EUR *(Frankfurt)* |

Stabilité Yahoo : 3 requêtes consécutives → prix identique (CHDIV).

## Verdict technique

### **GO WITH CONDITIONS** — scrape viable, mais **pas via UBS CH tel que spécifié dans le PRD v2**

### Ce qui bloque UBS

1. **Pages CH** : contenu prix derrière React `FundgateHandler` + geo gate — `Invoke-WebRequest` / Jsoup sur URL CH = **0 prix**.
2. **Pages NL** : données riches (table `Exchange | Bid | Ask` pour SIX) **quand** la page complète est servie — mais **403** depuis environnement script / probablement aussi VPS datacenter.
3. **Maintenance** : sélecteurs table markdown/HTML fragiles ; cookie wall possible.

### Ce qui fonctionne aujourd'hui

**Yahoo chart API** (sans clé, JSON stable) :

```
GET https://query1.finance.yahoo.com/v8/finance/chart/CHDIV.SW?interval=1d&range=1d
→ chart.result[0].meta.regularMarketPrice  (CHF)

GET https://query1.finance.yahoo.com/v8/finance/chart/QQQE.SW?interval=1d&range=1d
→ chart.result[0].meta.regularMarketPrice  (USD)
```

- Parsing trivial (Jackson / `RestClient`)
- Couvre **les deux** tickers SIX
- ~96 req/jour largement sous tout rate limit observé
- **Contre** : endpoint non documenté, ToS Yahoo floue, risque de blocage IP (moins probable à ce volume)

### Recommandation architecture (mise à jour PRD)

```
providers/scrape/
  ScrapePriceProvider
  parsers/
    YahooSixChartParser     # primaire recommandé post-spike
    UbsNlFundPageParser     # optionnel si VPS passe WAF + test prod
    DeutscheBoerseParser    # repli QQQE seulement (EUR, pas SIX)
```

Registre YAML :

```yaml
- id: chdiv
  provider: scrape
  scrape-parser: yahoo-six-chart
  symbol: CHDIV.SW

- id: qqqe
  provider: scrape
  scrape-parser: yahoo-six-chart
  symbol: QQQE.SW
```

**Note produit :** Dokk avait écarté `yfinance` (lib Python). L'endpoint Yahoo chart est une **requête HTTP JSON** sans dépendance yfinance — à valider explicitement comme acceptable ou non.

## Garde-fous implémentation (inchangés)

- Sync 10 min, heures `Europe/Zurich` 09:00–17:30 uniquement
- Cache dernier prix + `priceSource: cache` si échec
- Badge UI « prix source scrape »
- Tests unitaires avec fixtures JSON Yahoo (pas HTML UBS)
- Log `scrape_fail_rate` pour SM-5 (> 95 % succès)

## Gate avant dev

| # | Action | Owner |
|---|--------|-------|
| 1 | **Décision Dokk** : Yahoo chart OK vs insister UBS NL (test depuis VPS prod Docker) | Dokk |
| 2 | Test `spike-scrape-ubs.ps1` **depuis le VPS** `dashboard.dokkcorp.ch` | @steve |
| 3 | Si UBS NL 403 sur VPS → adopter `yahoo-six-chart` dans PRD addendum | @odin |
| 4 | Spike impl : parser + fixture JSON + test JUnit | @steve |

## Fichiers spike

| Fichier | Rôle |
|---------|------|
| `scripts/spike-scrape-ubs.ps1` | Script reproductible |
| `spike-scrape-results-YYYY-MM-DD.json` | Résultats JSON |
| `spike-scrape-fixtures/*.html` | HTML capturés (UBS, DB, justETF) |

## Plan B

Si Yahoo bloqué : saisie manuelle prix CHDIV/QQQE en config (comme fondamentaux) jusqu'à source alternative — le dashboard reste utilisable.
