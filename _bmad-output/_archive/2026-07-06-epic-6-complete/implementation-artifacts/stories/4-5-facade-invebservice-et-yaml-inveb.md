---
story_id: 4.5
story_key: 4-5-facade-invebservice-et-yaml-inveb
epic: 4
epic_name: UI générique et migration Investor AB
status: done
baseline_commit: 35ff7e0
created: 2026-06-30
FRs:
  - FR-14
dependencies:
  - 4.4 (Migration page InveB vers AssetPage)
---

# Story 4.5: Façade InveBService et YAML inveb

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

En tant que Dokk,
je veux qu'InveBService délègue au service générique,
afin de préserver les tests et endpoints legacy pendant la transition.

## Acceptance Criteria

### AC-1 : Données YAML actives
- Les configurations statiques `backend/src/main/resources/config/dividends/inveb.yml` et `backend/src/main/resources/config/fundamentals/inveb.yml` sont valides et lues correctement par `ConfigurableAssetService` (déjà en place).

### AC-2 : InveBService annoté @Deprecated et délégant
- La classe `InveBService` est marquée de l'annotation `@Deprecated`.
- `InveBService.getData()` et `InveBService.getLastInveBData()` délèguent entièrement à `ConfigurableAssetService.getData("inveb")` et effectuent le mapping du résultat `AssetDto` vers un `InveBDto`.
- Le cache en mémoire (`cachedData`, `historyPrices`, `historyDays`), le verrou local (`loadLock`) et le client direct `FMPClient` sont supprimés d'`InveBService`.

### AC-3 : Retrait de l'appel FMP direct et synchronisation via Registre
- Aucun appel API direct à FMP n'est effectué dans `InveBService`. La synchronisation régulière est déléguée au mécanisme global du registre (`AssetSyncJob.syncFmpAssets` qui appelle `ConfigurableAssetService.syncPrice("inveb")`).

### AC-4 : Adaptation des tests unitaires InveBServiceTest
- La classe de test `InveBServiceTest` est modifiée pour remplacer les mocks `FMPClient`, `AssetSnapshotRepository` et `AssetDailyRepository` par un mock de `ConfigurableAssetService`.
- Les tests vérifient la bonne délégation à `ConfigurableAssetService` et le mapping correct vers `InveBDto`.
- Tous les tests de la suite (`./mvnw.cmd test`) passent au vert.

### AC-5 : Maintien du Endpoint Legacy
- Le endpoint REST GET `/api/dashboard/inveb` dans `DashboardController` fonctionne toujours sans régression et retourne l'objet `InveBDto` correctement rempli depuis la façade.

## Tasks / Subtasks

- [x] Implémenter la délégation dans `InveBService` (AC-2, AC-3)
  - [x] Annoter `InveBService` avec `@Deprecated`
  - [x] Remplacer les champs `fmpClient`, `assetSnapshotRepository`, `assetDailyRepository` par `ConfigurableAssetService`
  - [x] Supprimer les variables de cache local (`cachedData`, `historyPrices`, `historyDays`, `lastHistoryRefresh`) et les verrous
  - [x] Mettre à jour `getData()` et `getLastInveBData()` pour appeler `configurableAssetService.getData("inveb")` et retourner un `InveBDto`
- [x] Mettre à jour les tests unitaires du backend (AC-4)
  - [x] Adapter le fichier [InveBServiceTest.java](file:///backend/src/test/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBServiceTest.java) pour mocker `ConfigurableAssetService`
  - [x] Valider le passage de la suite avec `./mvnw.cmd test`
- [x] Valider la compilation du contrôleur (AC-5)
  - [x] S'assurer que [DashboardController.java](file:///backend/src/main/java/com/dokkcorp/dashboard/controller/DashboardController.java) compile sans avertissements ou erreurs majeures

### Review Findings

- [x] [Review][Patch] Silent Exception Swallowing in getData() [backend/src/main/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBService.java:40-42]
- [x] [Review][Patch] Missing @Deprecated annotation on public methods [backend/src/main/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBService.java:18-22]
- [x] [Review][Patch] Missing unit test verifying delegation in getLastInveBData [backend/src/test/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBServiceTest.java:25]
- [x] [Review][Patch] Compiler deprecation warnings in DashboardController [backend/src/main/java/com/dokkcorp/dashboard/controller/DashboardController.java:26]
- [x] [Review][Patch] Hardcoded magic string "inveb" [backend/src/main/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBService.java:24]

## Dev Notes

### Architecture Patterns et Contraintes
- **Non-duplication** : Le cache, la logique des heures de marché (Stockholm) et les calculs d'historique ou de yield sont entièrement centralisés dans `ConfigurableAssetService`.
- **Mapping DTO** : En cas d'erreur ou d'absence de données, retourner `InveBDto.error("INVE-B")`.
- **Annotation** : Utiliser l'annotation standard `@Deprecated` sur la classe et les méthodes publiques d'`InveBService`.

### Source tree components to touch
- [InveBService.java](file:///backend/src/main/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBService.java) (UPDATE)
- [InveBServiceTest.java](file:///backend/src/test/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBServiceTest.java) (UPDATE)

### References
- Fichier de configuration du registre : [assets-registry.yml](file:///backend/src/main/resources/config/assets-registry.yml)
- Service cible : [ConfigurableAssetService.java](file:///backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java)
- Guide des règles du projet : [project-context.md](file:///docs/project-context.md)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium)

### Debug Log References

- Exécution réussie des 104 tests unitaires backend avec Maven wrapper.

### Completion Notes List

- `InveBService` est annoté `@Deprecated` et délègue à `ConfigurableAssetService.getData("inveb")`.
- Le mapping vers `InveBDto` est préservé, éliminant les dépendances FMP, de cache, et de base de données redondantes de la classe legacy.
- Les tests unitaires dans `InveBServiceTest.java` ont été simplifiés pour tester cette délégation de manière isolée via Mockito.

### File List

- `backend/src/main/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBService.java` (Modified)
- `backend/src/test/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBServiceTest.java` (Modified)
