---
story_id: 4.3
story_key: 4-3-template-assetpage-et-pricefreshnessbadge
epic: 4
epic_name: UI générique et migration Investor AB
status: review
baseline_commit: 35ff7e0
created: 2026-06-30
FRs:
  - FR-14
  - FR-16
dependencies:
  - 4.2 (Composants génériques DividendCard et FundamentalsCard)
---

# Story 4.3: Template AssetPage et PriceFreshnessBadge

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

En tant que Dokk,
je veux une page actif réutilisable avec badge de fraîcheur du prix,
afin de voir si le cours est live, stale ou marché fermé.

## Acceptance Criteria

### AC-1: Composant `PriceFreshnessBadge`
- Le composant `PriceFreshnessBadge` est créé sous `frontend/src/app/shared/components/price-freshness-badge/`.
- Il accepte en inputs :
  - `marketStatus` (`'OPEN' | 'CLOSED' | null`)
  - `priceSource` (`'FMP' | 'SCRAPE' | 'CACHE' | null`)
  - `lastRefresh` (Epoch milliseconds, `number | null`)
- Il affiche un badge visuel et textuel avec les règles suivantes :
  - **Closed** (Gris, ex. `bg-dark-700 text-copper-300/60`) : si `marketStatus === 'CLOSED'`. Texte : "Marché Fermé".
  - **Stale** (Orange/Rouge, ex. `bg-red-950/40 text-red-400 border border-red-500/20`) : si `priceSource === 'CACHE'` ou si l'âge de la donnée (`Date.now() - lastRefresh`) dépasse le seuil critique (seuil par défaut : 30 minutes, ou le double de l'intervalle de synchronisation attendu). Texte : "Différé" ou "Obsolète".
  - **Live** (Vert, ex. `bg-green-950/40 text-green-400 border border-green-500/20`) : si le marché est ouvert (`'OPEN'`), que la source est `'FMP'` ou `'SCRAPE'`, et que la donnée est fraîche (âge < 30 minutes). Texte : "En Direct".
- Le badge affiche à côté ou en infobulle le délai écoulé (ex. "Mis à jour il y a 2 min") ou la date et heure du rafraîchissement formatée en français.

### AC-2: Template `AssetPage` réutilisable
- Le composant `AssetPage` est créé sous `frontend/src/app/shared/components/asset-page/`.
- Il accepte un input signal `assetId: string`.
- Il appelle `DashboardApiService.getAsset(assetId)` à l'initialisation et rafraîchit les données toutes les 3 minutes (180 000 ms), avec nettoyage correct du timer via `DestroyRef`.
- Il affiche les composants suivants alimentés par le `AssetDto` :
  - Le `PriceFreshnessBadge`.
  - La carte principale existante `AssetMainCard`.
  - Le graphique annuel `PriceChart` et le graphique intraday `DailyChart`.
  - Les cartes génériques `DividendCard` et `FundamentalsCard` (ne s'affichent que si les blocs respectifs `dividends` et `fundamentals` ne sont pas nuls dans le DTO).
- Gère proprement le chargement et le cas où l'API renvoie `null` (affichage d'un indicateur de chargement ou message propre, et notification d'erreur via le service de toast existant).

### AC-3: Responsive et Thème
- Le template s'intègre parfaitement dans le thème sombre et cuivre existant (couleurs de la palette Tailwind uniquement).
- Responsive mobile-first (mise en page sur une seule colonne sur mobile, deux colonnes sur écrans larges).

### AC-4: Tests Unitaires
- Les tests unitaires Vitest `*.spec.ts` vérifient :
  - Le rendu des différents états du badge de fraîcheur (Live, Stale, Closed).
  - L'initialisation d'une page actif, la récupération des données de l'API, et l'affichage des graphiques et cartes selon la présence des blocs dans le DTO.

## Tasks / Subtasks

- [x] Créer le composant `PriceFreshnessBadge` (AC-1, AC-3)
  - [x] Créer le fichier [price-freshness-badge.ts](file:///frontend/src/app/shared/components/price-freshness-badge/price-freshness-badge.ts)
  - [x] Créer le fichier [price-freshness-badge.html](file:///frontend/src/app/shared/components/price-freshness-badge/price-freshness-badge.html)
- [x] Créer le template `AssetPage` (AC-2, AC-3)
  - [x] Créer le fichier [asset-page.ts](file:///frontend/src/app/shared/components/asset-page/asset-page.ts)
  - [x] Créer le fichier [asset-page.html](file:///frontend/src/app/shared/components/asset-page/asset-page.html)
- [x] Tests Unitaires (AC-4)
  - [x] Créer le fichier [price-freshness-badge.spec.ts](file:///frontend/src/app/shared/components/price-freshness-badge/price-freshness-badge.spec.ts)
  - [x] Créer le fichier [asset-page.spec.ts](file:///frontend/src/app/shared/components/asset-page/asset-page.spec.ts)
  - [x] Lancer la suite de tests et valider le succès

### Review Findings

- [x] [Review][Patch] F1: `takeUntilDestroyed` appelé dans `refresh()` hors contexte d'injection — crash runtime Angular [asset-page.ts:77]
- [x] [Review][Patch] F2: `if (!refresh)` trop permissif — `0` (epoch valide) traité comme null [price-freshness-badge.ts:55]
- [x] [Review][Patch] F3: Source `SCRAPE` non couverte dans les tests de `PriceFreshnessBadge` [price-freshness-badge.spec.ts]
- [x] [Review][Patch] F4: État de chargement non testé dans `asset-page.spec.ts` [asset-page.spec.ts]
- [x] [Review][Defer] F5: Titres des graphiques en anglais dans une UI française [asset-page.html:43,55] — deferred, pre-existing pattern
- [x] [Review][Defer] F6: `staleThresholdMs` exposé en tant qu'`input()` public — devrait être une constante interne [price-freshness-badge.ts:16] — deferred, pre-existing

## Dev Notes

### Architecture Patterns et Contraintes
- **Variables DTO directes** : Utiliser la propriété `marketStatus` du DTO pour déterminer l'état fermé du marché. Ne pas dupliquer ou hardcoder de calculs de fuseaux horaires ou d'heures d'ouverture côté client.
- **Signals Angular** : Utiliser exclusivement des `computed()` et `signal()` pour l'état interne et les transformations. Pas de `BehaviorSubject`.
- **Typage strict** : Aucun type `any` dans les fichiers TypeScript.
- **Réutilisation** : Ce composant doit rester neutre et ne contenir aucune constante métier propre à un actif particulier.

### References
- [Contexte Projet](file:///c:/Users/chju/Desktop/Dashboard/docs/project-context.md)
- [Fiche d'Architecture](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/architecture.md)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium)

### Debug Log References

- Exécution des tests réussie via `npm run test` (40/40 tests au total, incluant les nouveaux composants).

### Completion Notes List

- Implémentation du composant `PriceFreshnessBadge` permettant de calculer dynamiquement la fraîcheur des prix (états 'En Direct', 'Marché Fermé', 'Différé', 'Donnée Obsolète') à partir de `marketStatus`, `priceSource` et `lastRefresh` de l'actif.
- Implémentation du composant `AssetPage` qui centralise et affiche la carte principale, les graphiques annuel et intraday, et les cartes génériques de dividendes/fondamentaux si renseignées dans le DTO.
- Écriture des tests unitaires complets avec mock de `DashboardApiService` et vérifications des rendus dans le DOM pour tous les états possibles.

### File List

- `frontend/src/app/shared/components/price-freshness-badge/price-freshness-badge.ts`
- `frontend/src/app/shared/components/price-freshness-badge/price-freshness-badge.html`
- `frontend/src/app/shared/components/price-freshness-badge/price-freshness-badge.spec.ts`
- `frontend/src/app/shared/components/asset-page/asset-page.ts`
- `frontend/src/app/shared/components/asset-page/asset-page.html`
- `frontend/src/app/shared/components/asset-page/asset-page.spec.ts`
