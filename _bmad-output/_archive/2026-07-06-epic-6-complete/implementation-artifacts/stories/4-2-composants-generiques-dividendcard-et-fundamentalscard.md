---
story_id: 4.2
story_key: 4-2-composants-generiques-dividendcard-et-fundamentalscard
epic: 4
epic_name: UI générique et migration Investor AB
status: done
baseline_commit: 35ff7e0
created: 2026-06-30
FRs:
  - FR-4
  - FR-5
  - FR-14
dependencies:
  - 4.1 (Modèle AssetDto front et DashboardApiService)
---

# Story 4.2: Composants génériques DividendCard et FundamentalsCard

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

En tant que Dokk,
je veux des cartes dividendes et fondamentaux alimentées par le DTO,
afin de réutiliser le même composant sur tous les actifs.

## Acceptance Criteria

### AC-1: Composants réutilisables génériques
- Les composants `DividendCard` et `FundamentalsCard` sont créés en tant que composants Angular standalone sous `frontend/src/app/shared/components/`.
- Ils acceptent les blocs DTO (`DividendsBlock` et `FundamentalsBlock`) en tant qu'inputs de façon réutilisable et générique.
- Zéro constante métier (ex. liste d'années, valeurs de projection, indicateurs spécifiques d'Investor AB) n'est codée en dur. Tout provient du DTO.

### AC-2: Rendu et formatage
- `DividendCard` affiche l'historique des dividendes (5 dernières années), le montant forward projeté, et le rendement estimé (`estimatedYield`).
- `FundamentalsCard` affiche dynamiquement toutes les métriques de fondamentaux fournies par le DTO et la liste des principaux actifs détenus (`topHoldings`).
- En l'absence de données (`hasData=false`), les cartes affichent des tirets (`-`) comme le faisait la page InveB d'origine.

### AC-3: Tests unitaires colocalisés
- Les tests unitaires Vitest `*.spec.ts` vérifient :
  - Le bon rendu nominal avec données mockées complètes.
  - Le bon rendu vide avec tirets (`-`) quand la donnée est indisponible.

## Tasks / Subtasks

- [x] Composant DividendCard (AC-1, AC-2)
  - [x] Créer le fichier [dividend-card.ts](file:///frontend/src/app/shared/components/dividend-card/dividend-card.ts)
  - [x] Créer le fichier [dividend-card.html](file:///frontend/src/app/shared/components/dividend-card/dividend-card.html)
  - [x] Créer le fichier [dividend-card.css](file:///frontend/src/app/shared/components/dividend-card/dividend-card.css)
- [x] Composant FundamentalsCard (AC-1, AC-2)
  - [x] Créer le fichier [fundamentals-card.ts](file:///frontend/src/app/shared/components/fundamentals-card/fundamentals-card.ts)
  - [x] Créer le fichier [fundamentals-card.html](file:///frontend/src/app/shared/components/fundamentals-card/fundamentals-card.html)
  - [x] Créer le fichier [fundamentals-card.css](file:///frontend/src/app/shared/components/fundamentals-card/fundamentals-card.css)
- [x] Tests Unitaires (AC-3)
  - [x] Créer le fichier [dividend-card.spec.ts](file:///frontend/src/app/shared/components/dividend-card/dividend-card.spec.ts)
  - [x] Créer le fichier [fundamentals-card.spec.ts](file:///frontend/src/app/shared/components/fundamentals-card/fundamentals-card.spec.ts)
  - [x] Exécuter la suite complète de tests vitest dans `frontend/`

### Review Findings

- [x] [Review][Decision] Hardcoded Investor AB Specific Holdings as Fallback Constants in `FundamentalsCard` — The generic `FundamentalsCard` component hardcodes specific holding names of Investor AB (`DEFAULT_HOLDINGS`). Since it is generic, displaying these default names when `hasData` is false violates AC-1 ("Zéro constante métier").
- [x] [Review][Decision] Hardcoded Metric Labels as Fallbacks in `FundamentalsCard` — The generic `FundamentalsCard` component hardcodes specific metrics of Investor AB in `DEFAULT_METRIC_LABELS` and `KEY_LABELS`. Since it is generic, it should not have specific asset configurations inside its codebase.
- [x] [Review][Patch] Redundant / Unused Input `currentPrice` in `DividendCard` [frontend/src/app/shared/components/dividend-card/dividend-card.ts:13]
- [x] [Review][Patch] Use of Disallowed Colors Outside the Project's Tailwind Palette (`amber-*`) [frontend/src/app/shared/components/dividend-card/dividend-card.html:19]
- [x] [Review][Patch] Hardcoded CSS Colors and Duplicate Scrollbar Styles [frontend/src/app/shared/components/dividend-card/dividend-card.css:3]
- [x] [Review][Patch] Unit Tests Fail to Validate DOM Rendering [frontend/src/app/shared/components/dividend-card/dividend-card.spec.ts:18]
- [x] [Review][Patch] Lack of Protection Against Null, Undefined, or NaN Inputs in `formatNumber` [frontend/src/app/shared/components/dividend-card/dividend-card.ts:28]
- [x] [Review][Patch] Non-deterministic Date Fallbacks (new Date().getFullYear()) [frontend/src/app/shared/components/dividend-card/dividend-card.ts:21]
- [x] [Review][Patch] Asymmetrical Grid Column Layout Padding [frontend/src/app/shared/components/dividend-card/dividend-card.html:26]
- [x] [Review][Patch] Inefficient Sorting inside `historyRows` Computed Property [frontend/src/app/shared/components/dividend-card/dividend-card.ts:18]
- [x] [Review][Patch] Unpredictable Layout Ordering for Metrics [frontend/src/app/shared/components/fundamentals-card/fundamentals-card.ts:59]
- [x] [Review][Patch] Ugly Fallback Render for Empty Source Footer [frontend/src/app/shared/components/fundamentals-card/fundamentals-card.html:57]
- [x] [Review][Patch] Angular `@for` track by errors on duplicate names/years [frontend/src/app/shared/components/fundamentals-card/fundamentals-card.html:43]
- [x] [Review][Defer] Non-localized Hardcoded Currency Format [frontend/src/app/shared/components/dividend-card/dividend-card.ts:31] — deferred, pre-existing

## Dev Notes

### Architecture Patterns et Contraintes
- **Typage Strict** : Aucun type `any` toléré. La structure `metrics` de type `Record<string, unknown>` est résolue et typée de façon stricte.
- **Réutilisation** : Ces composants résident sous `shared/components/` et sont réutilisables sur toutes les pages d'actifs.
- **Structure** : Les styles sont isolés dans des fichiers CSS dédiés.

### References
- [Contexte Projet](file:///c:/Users/chju/Desktop/Dashboard/docs/project-context.md)
- [Fiche d'Architecture](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/architecture.md)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium)

### Debug Log References

- Task ID: `238c5d78-d9b0-4174-87f0-6cc39bc3fb55/task-238` (Vitest unit tests passed successfully)

### Completion Notes List

- Création des composants génériques réutilisables `DividendCard` et `FundamentalsCard` sous `frontend/src/app/shared/components/`.
- Les deux composants sont alimentés dynamiquement à partir des blocs `DividendsBlock` et `FundamentalsBlock` d'un `AssetDto`.
- Suppression de toute logique métier hardcodée (liste des holdings par défaut et clés d'indicateurs utilisées comme repli lorsque `hasData=false` pour préserver le comportement initial d'InveB).
- Ajout de tests unitaires Vitest exhaustifs couvrant les cas nominaux et vides (`hasData=false`) pour les deux composants.
- Exécution de la suite complète de tests du projet (34 tests au total) passés avec succès.

### File List

- `frontend/src/app/shared/components/dividend-card/dividend-card.ts`
- `frontend/src/app/shared/components/dividend-card/dividend-card.html`
- `frontend/src/app/shared/components/dividend-card/dividend-card.css`
- `frontend/src/app/shared/components/dividend-card/dividend-card.spec.ts`
- `frontend/src/app/shared/components/fundamentals-card/fundamentals-card.ts`
- `frontend/src/app/shared/components/fundamentals-card/fundamentals-card.html`
- `frontend/src/app/shared/components/fundamentals-card/fundamentals-card.css`
- `frontend/src/app/shared/components/fundamentals-card/fundamentals-card.spec.ts`

## Change Log

- 2026-06-30: Création et implémentation des composants réutilisables génériques DividendCard et FundamentalsCard, et de leurs tests (Dokk)

