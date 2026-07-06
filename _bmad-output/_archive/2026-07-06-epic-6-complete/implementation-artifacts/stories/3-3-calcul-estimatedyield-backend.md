---
story_id: 3.3
story_key: 3-3-calcul-estimatedyield-backend
epic: 3
epic_name: Dividendes, fondamentaux et alertes trimestrielles
status: done
baseline_commit: 35ff7e0
created: 2026-06-25
FRs:
  - FR-4
NFRs:
  - NFR-12
dependencies:
  - 3.1 (Configuration dividendes YAML)
  - 1.4 (Modèle AssetDto unifié)
---

# Story 3.3: Calcul estimatedYield backend

Status: done

## Story

En tant que Dokk,
je veux que le rendement dividende soit calculé sur le prix live,
afin de ne plus hardcoder 6 SEK côté front.

## Acceptance Criteria

### AC-1: Calcul dynamique de estimatedYield dans DividendsBlock
**Given** `forwardDividend` configuré pour un actif et `currentPrice` disponible lors de la construction de l'`AssetDto`
**When** `ConfigurableAssetService` assemble l'`AssetDto` (dans `getData` ou `syncPrice`)
**Then** le champ `dividends.estimatedYield` est calculé selon la formule :
  `estimatedYield = (forwardDividend / currentPrice) * 100`
**And** la valeur est stockée sous forme de `BigDecimal` avec une échelle de 2 (`scale = 2`) et un arrondi `HALF_UP` (NFR-12).

### AC-2: Gestion de la nullité du prix ou du dividende
**Given** un actif sans `forwardDividend` configuré (le bloc `dividends` est nul ou inexistant)
**When** l'`AssetDto` est assemblé
**Then** `dividends` est nul.
**Given** un actif avec `forwardDividend` configuré mais un `currentPrice` nul ou absent dans l'`AssetDto`
**When** l'`AssetDto` est assemblé
**Then** le champ `dividends.estimatedYield` est positionné à `null` pour éviter toute division par zéro.

### AC-3: Intégration dans ConfigurableAssetService
**Given** l'interface ou la classe `ConfigurableAssetService`
**When** elle assemble le DTO via `getData` ou `syncPrice`
**Then** elle appelle une méthode publique ou privée `computeEstimatedYield(String assetId, BigDecimal currentPrice)` (selon ADR-06 / Fiche d'Architecture) pour encapsuler le calcul
**And** le DTO final expose la valeur calculée dans le bloc `dividends`.

### AC-4: Tests unitaires du calcul
**Given** des scénarios avec des prix et des dividendes de test (par exemple, prix = 100.00 et forwardDividend = 5.00)
**When** les tests de `ConfigurableAssetServiceTest` sont exécutés
**Then** ils vérifient que `estimatedYield` est correctement calculé à `5.00`
**And** vérifient le cas de division par zéro (prix nul) retournant `null`
**And** la suite de tests Maven `./mvnw.cmd test` passe avec succès.

## Tasks / Subtasks

- [x] Implémenter la logique de calcul de estimatedYield (AC-1, AC-2, AC-3)
  - [x] Ajouter la méthode `public BigDecimal computeEstimatedYield(String assetId, BigDecimal currentPrice)` dans `ConfigurableAssetService`
  - [x] Mettre à jour `buildDividendsBlock` dans `ConfigurableAssetService` pour accepter le `currentPrice` (sous forme de `Double`) et calculer `estimatedYield`
  - [x] Adapter les appels à `buildDividendsBlock` dans `syncPrice` et `assembleDto`
- [x] Écrire les tests unitaires et valider (AC-4)
  - [x] Ajouter des tests unitaires dans `ConfigurableAssetServiceTest` pour les différents scénarios de calcul de `estimatedYield` (cas normal, cas prix nul, cas dividende nul)
  - [x] Lancer `./mvnw.cmd test` à la racine de `/backend` pour s'assurer du succès complet des tests

### Review Findings

- [x] [Review][Patch] Calcul manquant d'estimatedYield dans getData et assembleDto [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:116]
- [x] [Review][Patch] Bloc dividends non nul quand forwardDividend n'est pas configuré [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:335]
- [x] [Review][Patch] Manque de try-catch autour de refreshHistory dans syncPrice [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:123]
- [x] [Review][Defer] I/O réseau bloquante dans le bloc synchronized de syncPrice [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:122] — deferred, pre-existing
- [x] [Review][Defer] Utilisation de la réflexion dans les tests unitaires [backend/src/test/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetServiceTest.java:18] — deferred, pre-existing
- [x] [Review][Defer] Mises à jour non atomiques des champs volatiles dans HistoryState [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:413] — deferred, pre-existing
- [x] [Review][Defer] Suivi incohérent des métriques d'échec des providers dans syncPrice [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:179] — deferred, pre-existing
- [x] [Review][Defer] Utilisation de sources de temps incohérentes [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:207] — deferred, pre-existing
- [x] [Review][Defer] Conditions de concurrence lors de l'éviction dynamique du cache [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:93] — deferred, pre-existing
- [x] [Review][Defer] Requêtes base de données lors d'opérations en lecture seule [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:111] — deferred, pre-existing
- [x] [Review][Defer] Risque de NullPointerException dans les builders de blocs de propriétés [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:335] — deferred, pre-existing
- [x] [Review][Defer] Absence de garde pour dailyPoints null retourné par le repository [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:261] — deferred, pre-existing
- [x] [Review][Defer] Absence de garde contre les éléments null dans la liste des points quotidiens [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:283] — deferred, pre-existing
- [x] [Review][Defer] Risque de NumberFormatException lors de la conversion de prix Double.NaN/Infinite [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:348] — deferred, pre-existing
- [x] [Review][Defer] Risque de NullPointerException si la map de métriques contient des valeurs null [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:385] — deferred, pre-existing
- [x] [Review][Defer] Absence de garde contre les ID d'actifs null dans le registre [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:93] — deferred, pre-existing

## Dev Notes

### Patterns et Contraintes d'Architecture
- **Mathématiques et Précision** : Utiliser exclusivement `BigDecimal` pour les divisions financières afin d'éviter les dérives sur les virgules flottantes (`double`). Utiliser `RoundingMode.HALF_UP`.
- **Conversion Double vers BigDecimal** : Pour convertir le prix live `Double` en `BigDecimal`, utiliser de préférence `BigDecimal.valueOf(double)` ou `new BigDecimal(String.valueOf(double))` pour éviter les bruits de représentation binaire.
- **Lookup insensible à la casse** : Toujours normaliser l'identifiant de l'actif avec `.toLowerCase(Locale.ROOT)`.

### Références
- [Contexte du projet](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/project-context.md)
- [Fiche d'Architecture (ADR-06)](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/architecture.md#L226)
- [Fiche d'Architecture - Section 5 (Service métier)](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/architecture.md#L570)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium)

### Debug Log References

- Task ID: `5ff38ed6-4041-4cdf-b06c-0c4539885b21/task-198` (mvn test pass)

### Completion Notes List

- Implemented `computeEstimatedYield(String, BigDecimal)` in `ConfigurableAssetService` returning `BigDecimal` with scale 2 and `RoundingMode.HALF_UP`
- Updated `buildDividendsBlock` signature to accept the live price and dynamically calculate `estimatedYield`
- Updated `syncPrice` to pass the live price to `buildDividendsBlock`
- Added unit tests in `ConfigurableAssetServiceTest` validating yield calculation under normal conditions, null price conditions, and missing configurations
- Verified that all unit tests build and pass successfully (96 tests passed)

## File List

- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java`
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetServiceTest.java`

## Change Log

- 2026-06-25: Implemented estimated yield calculation backend logic and tests (Dokk)
