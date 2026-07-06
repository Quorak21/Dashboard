---
baseline_commit: 35ff7e019c6faf63eee0585a9bda731d54513a27
---

# Story 2.2: AssetSyncJob — tier FMP registry-driven

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

En tant que Dokk,
je veux que le job de synchronisation FMP itère sur le registre d'actifs avec des planifications basées sur les offsets de minutes configurés,
afin de synchroniser tous les actifs FMP dynamiquement sans code spécifique par actif.

## Acceptance Criteria

1. **Given** le registre d'actifs configuré contenant les définitions des actifs FMP (ex: `inveb` dans la phase initiale).
2. **When** la tâche planifiée `syncFmpAssets` s'exécute selon un déclenchement régulier (ex: cron toutes les minutes ou cron spécifique comme `0 0/15 * * * ?` pour `inveb`).
3. **Then** chaque actif avec le provider `FMP` est synchronisé en appelant `ConfigurableAssetService.syncPrice(assetId)` uniquement si le marché est ouvert (`MarketHoursGuard.isOpen(asset)` est vrai).
4. **And** les échecs de synchronisation sur un actif sont isolés par un bloc `try-catch` individuel et loggés comme `ERROR`, sans bloquer la synchronisation des autres actifs.
5. **And** la synchronisation automatique de HYPE (`syncHype`) reste active toutes les 10 minutes (`0 0/10 * * * ?` appelant `hypeService.getData()`), inchangée.
6. **And** l'ancienne méthode `autoSync()` de `AssetSyncJob` ne contient plus d'appel à `inveBService.getData()` (l'injection de `InveBService` peut être supprimée de `AssetSyncJob` si elle n'est plus utilisée).

## Tasks / Subtasks

- [x] Injecter les dépendances requises dans `AssetSyncJob` (AC: #3)
  - [x] Injecter `ConfigurableAssetService`, `AssetRegistry` et `MarketHoursGuard` dans le constructeur de `AssetSyncJob`.
  - [x] Supprimer l'injection de `InveBService` de `AssetSyncJob` si elle n'est plus nécessaire à d'autres fins (attention aux autres méthodes comme `sendInveBSnapshot`, à adapter ou conserver l'injection uniquement pour cela).
- [x] Mettre à jour la méthode `autoSync()` (AC: #5, #6)
  - [x] Supprimer l'appel à `inveBService.getData()`.
  - [x] Conserver uniquement l'appel à `hypeService.getData()` sur le cron toutes les 10 minutes (`0 0/10 * * * ?`).
- [x] Implémenter la synchronisation planifiée des actifs FMP `syncFmpAssets` (AC: #2, #3, #4)
  - [x] Créer une méthode planifiée `@Scheduled(cron = "0 0/15 * * * ?")` (cron pour `inveb` avec offset 0).
  - [x] Récupérer tous les actifs du registre associés au provider `FMP` (`assetRegistry.byProvider(AssetProvider.FMP)`).
  - [x] Pour chaque actif FMP, vérifier si le marché est ouvert via `marketHoursGuard.isOpen(asset)`.
  - [x] Si oui, appeler `configurableAssetService.syncPrice(asset.id())` dans un try-catch individuel pour éviter qu'une exception ne bloque les autres actifs.
  - [x] Logger les erreurs de synchronisation de manière explicite au niveau `ERROR`.
- [x] Mettre à jour les tests unitaires (AC: #3, #4)
  - [x] Adapter le constructeur de `AssetSyncJob` dans `AssetSyncJobTest.java`.
  - [x] Ajouter un test pour `syncFmpAssets` validant le cas nominal (marché ouvert → appel à `syncPrice`), le cas hors heures (marché fermé → pas d'appel à `syncPrice`), et l'indépendance aux erreurs (un plantage d'un actif n'arrête pas le traitement des autres).
  - [x] Exécuter `./mvnw.cmd test` pour vérifier que tout est vert.

### Review Findings

- [x] [Review][Patch] Null Safety Defect on Asset in Loop [backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java:74-79]
- [x] [Review][Patch] Null Safety check on FMP assets list [backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java:71-72]
- [x] [Review][Patch] Mocking Java Records in Unit Tests [backend/src/test/java/com/dokkcorp/dashboard/jobs/AssetSyncJobTest.java:111-114]
- [x] [Review][Defer] Sequential External API calls blocking Scheduled Task Executor [backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java:71-82] — deferred, pre-existing
- [x] [Review][Defer] Lack of rate limiting/throttling for external calls [backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java:71-82] — deferred, pre-existing

## Dev Notes

- **FMP Cron & Offsets** : Actuellement, seul `inveb` (offset 0) est enregistré. Les autres actifs FMP seront rajoutés à l'Epic 6. Pour respecter la conception à terme, la méthode `syncFmpAssets` peut être planifiée à la minute (`0 * * * * ?`) en filtrant en Java si l'actif est dû selon `(minute - offset) % interval == 0` où `minute` est la minute courante. Alternativement, utiliser une planification fixe `0 0/15 * * * ?` pour cette story est acceptable.
- **Try-Catch par Actif** : Reprendre le même modèle de cloisonnement que `sendDailySnapshotToDb` afin qu'un échec réseau ou provider sur `inveb` ne perturbe pas les futurs actifs comme `brwm` ou `o`.
- **InveBService** : `InveBService` est déprécié mais est encore utilisé par `sendInveBSnapshot()` pour générer le snapshot quotidien à minuit. L'injection d' `InveBService` dans `AssetSyncJob` doit donc être conservée uniquement pour cette méthode, jusqu'à ce que la Story 2.4 vienne migrer la génération des snapshots vers le registre générique.

### Project Structure Notes

- Fichiers impliqués :
  - `backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java` (Modification)
  - `backend/src/test/java/com/dokkcorp/dashboard/jobs/AssetSyncJobTest.java` (Modification)

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-2.2](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/epics.md#L278-L294)
- [Source: docs/project-context.md](file:///c:/Users/chju/Desktop/Dashboard/docs/project-context.md)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium) / Antigravity

### Debug Log References

- compilation and tests verified successfully via `./mvnw.cmd test` (all 66 tests passing)

### Completion Notes List

- Injected `ConfigurableAssetService`, `AssetRegistry`, and `MarketHoursGuard` into `AssetSyncJob`.
- Maintained `InveBService` injection since it's still used in `sendInveBSnapshot`.
- Cleaned up `autoSync()` to only call `this.hypeService.getData()`.
- Implemented `syncFmpAssets()` with 15-minute cron scheduling, extracting assets from the registry by provider FMP, verifying market state with `marketHoursGuard.isOpen(asset)`, calling `configurableAssetService.syncPrice(asset.id())`, and wrapping each asset in a separate try-catch block logging failures with level `ERROR`.
- Verified execution of all unit tests.

### File List

- `backend/src/main/java/com/dokkcorp/dashboard/jobs/AssetSyncJob.java`
- `backend/src/test/java/com/dokkcorp/dashboard/jobs/AssetSyncJobTest.java`

## Change Log

- Refactoring of `AssetSyncJob` to make it registry-driven for FMP price sync.
- Added `syncFmpAssets` method scheduled on `0 0/15 * * * ?`.
