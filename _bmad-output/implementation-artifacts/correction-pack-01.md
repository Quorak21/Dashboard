# Correction Pack 01 — Post épic 6

> Pack de corrections priorisées, triées depuis l’archive deferred-work.  
> **Pack 00 (fait le 2026-07-06)** : fix O — warm-up cache + fundamentals YAML + sync FMP hors heures.

---

## Pack 00 — Fait ✅

| ID | Correction | Fichiers |
|----|------------|----------|
| P0-1 | `getData` déclenche `syncPrice` sur cache miss (prix O même marché fermé) | `ConfigurableAssetService.java` |
| P0-2 | `assembleDto` charge toujours fundamentals depuis YAML | `ConfigurableAssetService.java` |
| P0-3 | `syncFmpAssets` sync tous les actifs FMP (persist daily reste conditionné au marché ouvert) | `AssetSyncJob.java` |

**Déploiement requis** pour voir l’effet en prod.

---

## Pack 01 — Hygiène & UX — Fait ✅ (2026-07-06)

| ID | Correction | Statut |
|----|------------|--------|
| P1-1 | Titres graphiques en français (`Performance annuelle`, `Cours du jour`) | ✅ `asset-page.html` |
| P1-2 | `toastService.ts` → `toast.service.ts` + imports | ✅ |
| P1-3 | `staleThresholdMs` constante interne (30 min) | ✅ `price-freshness-badge.ts` |
| P1-4 | Fixture test `assets-registry-fixture.yml` (plus de copie prod) | ✅ |

---

## Pack 02 — Robustesse sync & perf — Fait ✅ (2026-07-06)

| ID | Correction | Statut |
|----|------------|--------|
| P2-1 | Respect `sync.interval-minutes` / `offset-minutes` (cron chaque minute + `isDueForSync`) | ✅ `AssetSyncJob.java` |
| P2-2 | Warm-up au démarrage (`ApplicationReadyEvent`) | ✅ |
| P2-3 | Throttle 500 ms entre appels FMP dans le job | ✅ |
| P2-4 | I/O réseau hors `synchronized` dans `syncPrice` | ✅ `ConfigurableAssetService.java` |
| P2-5 | Snapshots registre : `findLatestBySymbols` batch (évite N+1) | ✅ `AssetDailyRepository.java` |

---

## Pack 03 — Qualité & tests — Fait ✅ (2026-07-06)

| ID | Correction | Statut |
|----|------------|--------|
| P3-1 | Tests dashboard : erreur API + intervalle refresh 3 min | ✅ `dashboard.spec.ts` |
| P3-2 | `testGetLastHypeData` : assert payload JSON | ✅ `DashboardControllerTest.java` |
| P3-3 | Mock `ResizeObserver` global dans setup Vitest | ✅ `test-setup.ts` (+ stub dans specs chart) |
| P3-4 | Formatage devise via `Intl` (`formatCurrency`) | ✅ `format-number.ts`, `dividend-card.ts` |

---

## Pack 04 — Architecture (effort élevé, optionnel) — Non fait

| ID | Sujet | Notes |
|----|-------|-------|
| P4-1 | Observabilité Micrometer (au-delà SLF4J) | Seulement si besoin monitoring |
| P4-2 | Pagination `/alerts/quarterly` | Inutile à ~10 actifs |
| P4-3 | Auth Spring Security | Décision produit — actuellement accepté ad vitam |

---

## Ordre recommandé

1. ~~Déployer Pack 00~~ en prod
2. ~~Pack 01~~ ✅
3. ~~Pack 02~~ ✅
4. ~~Pack 03~~ ✅
5. Pack 04 au fil de l’eau (si besoin produit)

---

## Items archive — obsolètes (ne pas refaire)

- Wrappers Angular par actif (`/o`, `/brwm`…) → résolu (routage `/asset/:id`)
- Dashboard hardcodé `['inveb','brwm','o']` → résolu (rendu dynamique)
- `displayNamesByAssetId` → résolu
- Copy-paste `refresh()` 5 blocs → résolu
