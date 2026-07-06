---
type: spike-report
date: 2026-06-17
status: partial-fmp-complete
fmp_tested: true
finnhub_tested: false
blocked_by: "FINNHUB_API_KEY manquante — requise pour CHDIV, QQQE UCITS et dividendes/ratios"
script: scripts/spike-providers.ps1
---

# Spike providers — Résultats (2026-06-17)

## Résumé exécutif

| Provider | Statut | Verdict |
|----------|--------|---------|
| **FMP** (ton plan actuel) | ✅ Testé | **Prix OK** pour 5/7 actifs via `/stable/profile` |
| **FMP** dividendes / ratios / ETF | ❌ HTTP 402/401 | **Pas inclus** dans ton plan — pas de paiement possible |
| **Finnhub** | ⏸️ Non testé | **Clé requise** pour les 2 ETF suisses + métriques enrichies |

**Conclusion :** FMP seul suffit pour **prix + graphiques** sur 5 actifs. Les **2 ETF SIX** (`CHDIV`, `QQQE`) et les **dividendes live** (FR-4 PRD) nécessitent **Finnhub gratuit** ou restent manuels.

---

## Checklist actif par actif

| # | Actif | Symbole FMP | FMP profile | FMP div/ratios | Finnhub | **Go prix** | **Go métriques** |
|---|-------|-------------|-------------|----------------|---------|-------------|------------------|
| — | Investor AB | `INVE-B.ST` | ✅ 386.95 SEK | ❌ 402 | 🔲 | 🟢 | 🟡 FH |
| 1 | BlackRock World Mining | `BRWM.L` | ✅ 1027.6 GBp | ❌ 402 | 🔲 | 🟢 | 🟡 FH |
| 2 | Realty Income | `O` | ✅ 62.17 USD | ❌ 402 | 🔲 | 🟢 | 🟡 FH |
| 3 | 3i Group | `III.L` | ✅ 2307 GBp | 🔲 | 🔲 | 🟢 | 🟡 FH |
| 4 | UBS Swiss Dividend ETF | `CHDIV.SW` | ❌ vide | ❌ | 🔲 | 🔴 | 🔴 FH requis |
| 5 | Nasdaq 100 ETF UCITS | `QQQE.SW` | ❌ vide | ❌ | 🔲 | 🔴 | 🔴 FH requis |
| 6 | Global Infra ETF | `INFR.L` | ✅ 2907 GBp | ❌ 402 | 🔲 | 🟢 | 🟡 FH |

### Piège documenté : `QQQE` sans suffixe

FMP `search-symbol?query=QQQE` retourne **Direxion NASDAQ-100 Equal Weighted** (ETF US) — **pas** ton UBS UCITS `IE0003RQ9F90`. Ne jamais utiliser `QQQE` seul.

### ETF suisses absents de FMP

`CH1244681594`, `IE0003RQ9F90`, `CHDIV` → **0 résultat** sur `search-symbol`. FMP ne couvre pas ces lignes sur ton plan.

---

## Détail tests FMP (2026-06-17)

### ✅ `/stable/profile` — fonctionne

| Symbole | Prix | Devise | Market cap |
|---------|------|--------|------------|
| INVE-B.ST | 386.95 | SEK | 1.19T |
| BRWM.L | 1027.6 | GBp | 1.92B |
| O | 62.17 | USD | 58.0B |
| III.L | 2307 | GBp | 23.3B |
| INFR.L | 2907 | GBp | 1.51B |

Champs `lastDiv` vides sur profile — dividendes non fournis ici.

### ❌ Endpoints premium / hors plan

| Endpoint | Code | Impact PRD |
|----------|------|------------|
| `/stable/dividends` | **402** | FR-4 InveB live dividendes |
| `/stable/ratios` | **402** | FR-5 fondamentaux |
| `/stable/key-metrics` | **401** | P/E, NAV |
| `/stable/etf/info`, `/etf/holdings` | **401** | FR-9 à FR-11 ETF |
| `/api/v3/*` (legacy) | **403** | Migré vers stable |

**Interprétation :** ton plan FMP actuel = **prix de base** (profile). Pas d’upgrade payant → **Finnhub free** pour dividendes, ratios, ETF ISIN.

---

## Finnhub — oui, il te faut une clé

**Gratuit** sur https://finnhub.io/register (60 req/min, usage perso).

Sans Finnhub :
- ❌ Pas de `CHDIV` ni `QQQE` UCITS dans le dashboard
- ❌ Pas de dividendes live (sauf hardcode / saisie manuelle)
- ❌ Pas de sector donut ETF fiable

Avec Finnhub (à tester quand tu as la clé) :
```powershell
$env:FINNHUB_API_KEY='ta_cle'
$env:FMP_API_KEY='...'   # ne pas commiter
powershell -ExecutionPolicy Bypass -File scripts\spike-providers.ps1
```

---

## Architecture 0 € révisée post-spike

```
HYPE     → CoinGecko + Hyperliquid (inchangé)
Prix T0  → FMP /profile (5 actifs) + Finnhub quote (CHDIV, QQQE)
Div/T2   → Finnhub uniquement (FMP 402 sur ton plan)
ETF T3   → Finnhub etf/profile + sector-exposure par ISIN
```

---

## Prochaine action

1. Créer clé **Finnhub** gratuite
2. Relancer `scripts/spike-providers.ps1` (ou me donner la clé Finnhub pour finir le spike)
3. `bmad-prd` Update avec ces go/no-go réels

---

*Spike FMP exécuté 2026-06-17 — clé non stockée dans le dépôt.*
