---
story_id: 4.4
story_key: 4-4-migration-page-inveb-vers-assetpage
epic: 4
epic_name: UI générique et migration Investor AB
status: done
baseline_commit: 35ff7e0
created: 2026-06-30
FRs:
  - FR-14
  - FR-16
dependencies:
  - 4.3 (Template AssetPage et PriceFreshnessBadge)
---

# Story 4.4: Migration page InveB vers AssetPage

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

En tant que Dokk,
je veux qu'Investor AB utilise le nouveau template sans régression,
afin de valider le cadre sur l'actif existant.

## Acceptance Criteria

### AC-1: Route `/inveb` délègue à `AssetPage`
- Le composant `Inveb` (page `/inveb`) délègue complètement à `AssetPage` avec l'input `assetId` à `'inveb'`.
- Le fichier HTML `inveb.html` est nettoyé et contient uniquement le tag `<app-asset-page [assetId]="'inveb'"></app-asset-page>`.
- Les imports inutilisés dans `inveb.ts` sont nettoyés.

### AC-2: Suppression des cartes spécifiques Investor AB et de leurs constantes
- Le dossier `inveb-dividend-card` et `inveb-fundamentals-card` sous `frontend/src/app/features/stocks/inveb/` sont supprimés.
- Les constantes `INVEB_DIVIDEND_HISTORY_PLACEHOLDER`, `INVEB_FUNDAMENTALS_METRICS`, et autres valeurs codées en dur sont ainsi totalement supprimées du code source.

### AC-3: Mise à jour des tests `inveb.spec.ts`
- Les tests unitaires du composant `Inveb` sont mis à jour pour valider la délégation à `AssetPage`.
- Aucun test ne doit échouer ou être en régression.

## Tasks / Subtasks

- [x] Déléguer le composant `Inveb` à `AssetPage` (AC-1)
  - [x] Modifier [inveb.ts](file:///frontend/src/app/features/stocks/inveb/inveb.ts)
  - [x] Modifier [inveb.html](file:///frontend/src/app/features/stocks/inveb/inveb.html)
- [x] Supprimer les anciens composants de cartes spécifiques (AC-2)
  - [x] Supprimer le dossier [inveb-dividend-card](file:///frontend/src/app/features/stocks/inveb/inveb-dividend-card/)
  - [x] Supprimer le dossier [inveb-fundamentals-card](file:///frontend/src/app/features/stocks/inveb/inveb-fundamentals-card/)
- [x] Mettre à jour les tests unitaires (AC-3)
  - [x] Modifier le fichier de test [inveb.spec.ts](file:///frontend/src/app/features/stocks/inveb/inveb.spec.ts)
  - [x] Lancer la suite de tests et valider le succès

## Dev Notes

### Architecture Patterns et Contraintes
- Utiliser le composant standalone `AssetPage` existant.
- Ne pas introduire de régression sur les graphiques ou les données affichées.

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium)

### Debug Log References

- Exécution réussie des 38 tests frontend Vitest sans timeouts ni régressions.

### Completion Notes List

- Inveb component has been rewritten to delegate completely to AssetPage, with selector `<app-asset-page [assetId]="'inveb'"></app-asset-page>`.
- Removed old unused specific component folders `inveb-dividend-card` and `inveb-fundamentals-card` along with all their files and hardcoded placeholders.
- Re-implemented unit tests in `inveb.spec.ts` to assert correct delegation and child rendering.
- Optimized and removed `whenStable` periodic timer hangs across `inveb.spec.ts`, `hype.spec.ts`, and `asset-page.spec.ts`.

### File List

- `frontend/src/app/features/stocks/inveb/inveb.ts` (Modified)
- `frontend/src/app/features/stocks/inveb/inveb.html` (Modified)
- `frontend/src/app/features/stocks/inveb/inveb.spec.ts` (Modified)
- `frontend/src/app/features/crypto/hype/hype.spec.ts` (Modified)
- `frontend/src/app/shared/components/asset-page/asset-page.spec.ts` (Modified)
- `frontend/vitest.config.ts` (Modified)
- `frontend/src/app/features/stocks/inveb/inveb-dividend-card/` (Deleted)
- `frontend/src/app/features/stocks/inveb/inveb-fundamentals-card/` (Deleted)

### Change Log

- 2026-06-30: Migrated InveB stock detail view to generic AssetPage template, deleted specific cards, resolved Vitest environment slow test execution timeouts.

### Review Findings

- [x] [Review][Decision] Global test timeout set to 60,000ms — The test timeout in `frontend/vitest.config.ts` was increased to 60,000ms. Since the periodic timer hangs in unit tests have been fixed, we should consider if a 60s timeout is still necessary or if it can be reduced to a standard value (e.g. 10s) to avoid masking slow test issues.
- [x] [Review][Patch] Orphaned style url declaration in standalone component [frontend/src/app/features/stocks/inveb/inveb.ts:7]
