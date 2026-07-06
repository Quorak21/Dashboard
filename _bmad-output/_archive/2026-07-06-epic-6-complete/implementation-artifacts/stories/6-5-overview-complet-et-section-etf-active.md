---
story_id: 6.5
story_key: 6-5-overview-complet-et-section-etf-active
epic: 6
epic_name: Portefeuille élargi — actions, ETF FMP et overview complet
status: done
baseline_commit: 35ff7e0
created: 2026-07-02
FRs:
  - FR-15
dependencies:
  - 6.4 (iShares Global Infrastructure (infr) et EtfSectorChart)
---

# Story 6.5: Overview complet et section ETF active

Status: done

## Story

En tant que Dokk,
je veux voir toutes les cartes actifs sur l'accueil avec la section ETF débloquée,
afin d'avoir une vue portefeuille en un coup d'œil.

## Acceptance Criteria

### AC-1 : Cartes overview pour tous les actifs opérationnels
- **Given** les 5 actifs du registre (`inveb`, `brwm`, `o`, `iii`, `infr`) et HYPE configurés.
- **When** Dokk ouvre `/` (dashboard home).
- **Then** 5 cartes d'actifs hors HYPE affichent le prix courant, la variation 24h, la devise et le lien de détail de l'actif.
- **And** le refresh automatique du frontend toutes les 3 minutes est maintenu.

### AC-2 : Déverrouillage de la section ETFs
- **Given** le dashboard home page.
- **When** la page est rendue.
- **Then** la section **ETFs** ne possède plus les classes `opacity-40 grayscale` ni le badge « COMING SOON ».
- **And** l'actif `infr` est rendu sous la section **ETFs** avec son style complet et ses données.

### AC-3 : Tests Automatisés
- **Given** les modifications sur le dashboard.
- **When** on lance `ng test` (Vitest).
- **Then** la suite de tests passe au vert, incluant les assertions de chargement et d'affichage pour `infr`.

## Tasks / Subtasks

- [x] **Mise à jour du composant Dashboard (AC-1, AC-2)**
  - [x] Modifier `frontend/src/app/features/dashboard/dashboard.ts` : ajouter le signal, les computed properties et la souscription pour `infr`.
  - [x] Mettre à jour `refresh` et `onDestroy` dans `dashboard.ts` pour gérer le cycle de vie de `infrSub`.
- [x] **Mise à jour du template du Dashboard (AC-2)**
  - [x] Déverrouiller la section `ETFs` dans `frontend/src/app/features/dashboard/dashboard.html` (retirer les classes d'opacité/nuances de gris, adapter les couleurs de police et de fond pour être conformes à la palette copper, et supprimer le badge "COMING SOON").
  - [x] Insérer le composant `<app-asset-dashboard-card>` pour `infr` dans la section `ETFs` déverrouillée.
- [x] **Mise à jour des Tests Dashboard (AC-3)**
  - [x] Mettre à jour `frontend/src/app/features/dashboard/dashboard.spec.ts` pour mocker l'appel de `infr` dans le service API simulé.
  - [x] Ajouter des assertions sur `infrPrice` et `infrChange` dans les tests unitaires.

## Dev Notes

- **Précautions RxJS** : Toujours se désabonner proprement de `infrSub` lors de la destruction du composant.
- **Design & Cohérence** : La section ETFs doit utiliser la même palette copper et les mêmes styles que la section Stocks (icône et titres colorés en accents copper/amber).

### References

- Dashboard component: [dashboard.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/features/dashboard/dashboard.ts)
- Dashboard template: [dashboard.html](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/features/dashboard/dashboard.html)
- Dashboard spec: [dashboard.spec.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/features/dashboard/dashboard.spec.ts)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium)

### Debug Log References

- Task-233 (Frontend tests passed)
- Task-239 (Backend tests passed)

### Completion Notes List

- Added dynamic signals, computed price/change values, and subscription management for `infr` ETF on the home page dashboard.
- Unlocked the ETFs category section in dashboard view, styling it with copper/amber palette consistent with Stocks, and removing "COMING SOON" badge.
- Inserted infr asset card inside the ETFs section.
- Updated dashboard unit tests to mock and assert infr data retrieval and representation.

### File List

- `frontend/src/app/features/dashboard/dashboard.ts`
- `frontend/src/app/features/dashboard/dashboard.html`
- `frontend/src/app/features/dashboard/dashboard.spec.ts`

### Review Findings

- [x] [Review][Decision] HYPE card still rendered on dashboard — resolved: interpretation A confirmed, HYPE is an additional card outside the 5-count; no change needed.
- [x] [Review][Patch] Dead import `takeUntilDestroyed` unused in dashboard.ts — removed [dashboard.ts:2]
- [x] [Review][Patch] `infr` currencySymbol hardcoded as `$` — fixed to `GBp` [dashboard.html:69-70]
- [x] [Review][Patch] Tests assert signals only — DOM assertion added: `querySelector('app-asset-dashboard-card[asset="infr"]')` [dashboard.spec.ts]
- [x] [Review][Defer] Copy-paste explosion — 5 identical `getAsset` blocks in refresh(), no abstraction; adding a new asset requires touching 6+ places [dashboard.ts:66-114] — deferred, pre-existing architectural pattern
- [x] [Review][Defer] Stale in-flight HTTP response race — unsubscribe() before resubscribe() does not cancel XHR at network level; prior response can overwrite new signal state [dashboard.ts:67-113] — deferred, pre-existing
- [x] [Review][Defer] Sequential unsubscribe in onDestroy — exception in first unsubscribe() leaves remaining 5 subs active [dashboard.ts:55-63] — deferred, theoretical (standard RxJS unsubscribe does not throw)
- [x] [Review][Defer] 6 simultaneous API errors → only last toast shown; prior 5 swallowed by toast timer reset [dashboard-api.service.ts:18-23] — deferred, pre-existing toast service limitation
- [x] [Review][Defer] No test coverage for error path or 3-min refresh interval behavior [dashboard.spec.ts] — deferred, out of story scope
- [x] [Review][Defer] Magic string asset IDs ('inveb', 'brwm', 'o', 'iii', 'infr') — no enum/constant, typo silently returns null [dashboard.ts] — deferred, pre-existing

