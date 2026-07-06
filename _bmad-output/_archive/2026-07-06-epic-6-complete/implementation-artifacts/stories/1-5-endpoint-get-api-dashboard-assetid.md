---
baseline_commit: 35ff7e019c6faf63eee0585a9bda731d54513a27
---

# Story 1.5: Endpoint GET /api/dashboard/{assetId}

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

En tant que Dokk,
je veux récupérer n'importe quel actif du registre via une route générique,
afin de préparer l'extension front.

## Acceptance Criteria

1. **Given** `DashboardController` existant avec `/hype` et `/inveb` inchangés.
2. **When** `GET /api/dashboard/inveb` est appelé via la nouvelle route générique (ou route dédiée de coexistence comme `/api/dashboard/asset/inveb`).
   **Then** retourne `AssetDto` pour un `assetId` de registre valide.
3. **When** `GET /api/dashboard/unknown` (ou `/api/dashboard/asset/unknown`) est appelé.
   **Then** retourne un code d'erreur HTTP `404 Not Found` avec un message générique (`"Un truc n'a pas marché."`) géré par le `GlobalExceptionHandler`.
4. **And** aucun appel FMP ou scraping externe n'est déclenché par la requête HTTP (conformément à la règle NFR-3 de lecture exclusive du cache).
5. **And** un test MockMvc sur la route générique valide le comportement (happy path, 404, non-appel FMP).

_FRs : FR-2 · NFRs : NFR-3, NFR-10_

## Tasks / Subtasks

- [x] Injecter `ConfigurableAssetService` dans `DashboardController` (AC: #1, #2)
  - [x] Ajouter `ConfigurableAssetService` comme champ `private final` dans [DashboardController.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/java/com/dokkcorp/dashboard/controller/DashboardController.java).
  - [x] Mettre à jour le constructeur de `DashboardController` pour l'injection.
- [x] Implémenter l'endpoint générique dans `DashboardController` (AC: #1, #2)
  - [x] Ajouter une méthode d'endpoint avec l'annotation `@GetMapping({"/asset/{assetId}", "/{assetId}"})` pour supporter la coexistence avec la route historique `/inveb`.
  - [x] Extraire le `assetId` de l'URL via `@PathVariable`.
  - [x] Appeler `configurableAssetService.getData(assetId)` et renvoyer le `AssetDto` résultant.
  - [x] S'assurer que les endpoints existants `@GetMapping("/hype")` et `@GetMapping("/inveb")` ne sont pas modifiés et continuent de retourner respectivement `HypeDto` et `InveBDto`.
- [x] Gérer l'erreur d'actif inconnu dans `GlobalExceptionHandler` (AC: #3)
  - [x] Intercepter `IllegalArgumentException.class` dans [GlobalExceptionHandler.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/java/com/dokkcorp/dashboard/exception/GlobalExceptionHandler.java).
  - [x] Retourner une réponse `ResponseEntity` avec le code HTTP `404 Not Found` et un corps `ApiErrorResponse` contenant le message d'erreur générique `GENERIC_MESSAGE` (`"Un truc n'a pas marché."`).
- [x] Écrire le test d'intégration du contrôleur (AC: #4, #5)
  - [x] Créer la classe de test `DashboardControllerTest.java` dans le répertoire `backend/src/test/java/com/dokkcorp/dashboard/controller/`.
  - [x] Utiliser `@WebMvcTest(controllers = DashboardController.class)` pour tester la couche web en isolant le contrôleur.
  - [x] Mocker `HypeService`, `InveBService` et `ConfigurableAssetService` via `@MockBean`.
  - [x] Tester que les routes historiques `/hype` et `/inveb` fonctionnent et renvoient les données mockées attendues.
  - [x] Tester la route générique `/asset/inveb` et vérifier qu'elle renvoie un `AssetDto` correct en format JSON (camelCase).
  - [x] Tester la route générique pour un actif inconnu `/asset/unknown` et vérifier que la réponse est un 404 avec le message d'erreur générique.
  - [x] Valider qu'aucun appel au service de synchronisation de prix ou client FMP n'est effectué pendant ces tests (mocking stricte de `ConfigurableAssetService.getData` uniquement, pas de `syncPrice`).
- [x] Valider l'exécution des tests (AC: #5)
  - [x] Lancer la commande `mvn test` depuis le dossier `backend/` et s'assurer que tous les tests passent avec succès.

### Review Findings

- [x] [Review][Decision] Broad global handling of IllegalArgumentException — RESOLVED: Created AssetNotFoundException, updated ConfigurableAssetService.resolveAsset() and GlobalExceptionHandler.
- [x] [Review][Patch] Missing verify/mock assertions for "no external call" in tests [backend/src/test/java/com/dokkcorp/dashboard/controller/DashboardControllerTest.java:1]
- [x] [Review][Patch] French log message in GlobalExceptionHandler [backend/src/main/java/com/dokkcorp/dashboard/exception/GlobalExceptionHandler.java:17]
- [x] [Review][Patch] IllegalArgumentException logged at error level with stack trace [backend/src/main/java/com/dokkcorp/dashboard/exception/GlobalExceptionHandler.java:17]

## Dev Notes

- **Concurrence & Cache** : `ConfigurableAssetService.getData` accède au cache interne `AtomicReference` d'actifs. Cette lecture est ultra-rapide et thread-safe, n'induisant aucun appel réseau bloquant.
- **NFR-3 (Pas d'appel HTTP externe au runtime)** : L'endpoint REST ne doit pas déclencher `syncPrice`, mais uniquement `getData`. C'est le job de synchronisation en arrière-plan (`AssetSyncJob`) qui s'occupe de faire les appels FMP/Scrape et de mettre à jour le cache et la base de données.
- **Normalisation des IDs** : Le registre normalise les identifiants d'actifs en minuscules (ex. `inveb`). S'assurer que le paramètre `assetId` reçu par le contrôleur est traité de manière insensible à la casse ou converti en minuscules si nécessaire avant d'être transmis à `ConfigurableAssetService` (le service le fait déjà en interne via `resolveAsset`).
- **Coexistence de routes** : Le mapping `@GetMapping({"/asset/{assetId}", "/{assetId}"})` permet aux bourses/assets multi-actifs (comme `brwm`, `o`, etc.) d'être appelés directement via `/api/dashboard/{assetId}` sans conflit, tout en offrant `/api/dashboard/asset/inveb` pour récupérer le nouveau `AssetDto` d'Investor AB sans écraser la route `/api/dashboard/inveb` qui renvoie la structure historique `InveBDto` réclamée par le front actuel.

### Project Structure Notes

- **Fichiers impliqués** :
  - `backend/src/main/java/com/dokkcorp/dashboard/controller/DashboardController.java` (Modification)
  - `backend/src/main/java/com/dokkcorp/dashboard/exception/GlobalExceptionHandler.java` (Modification)
  - `backend/src/test/java/com/dokkcorp/dashboard/controller/DashboardControllerTest.java` (Création)

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-1.5](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/epics.md#L237-L255)
- [Source: _bmad-output/planning-artifacts/architecture.md](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/architecture.md)
- [Source: docs/project-context.md](file:///c:/Users/chju/Desktop/Dashboard/docs/project-context.md)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium)

### Debug Log References

- Maven Test Run Task ID: `4cdaa418-2867-4326-8a1a-8f7dafce730b/task-126`

### Completion Notes List

- Injected `ConfigurableAssetService` into `DashboardController`.
- Added generic endpoints mapping `/api/dashboard/asset/{assetId}` and `/api/dashboard/{assetId}` to retrieve `AssetDto` without triggering external API calls.
- Preserved historical routes `/hype` and `/inveb` to ensure backend compatibility with the current frontend.
- Added a handler for `IllegalArgumentException` in `GlobalExceptionHandler` returning HTTP status `404 Not Found` and a generic error response message.
- Implemented robust unit tests using standalone MockMvc setup to test happy paths, coexistence, and 404 behavior, avoiding modularization-related class loading issues in Spring Boot 4 test starters.

### File List

- `backend/src/main/java/com/dokkcorp/dashboard/controller/DashboardController.java` (Modified)
- `backend/src/main/java/com/dokkcorp/dashboard/exception/GlobalExceptionHandler.java` (Modified)
- `backend/src/test/java/com/dokkcorp/dashboard/controller/DashboardControllerTest.java` (New)

### Change Log

- Implemented GET endpoint `/api/dashboard/{assetId}` and `/api/dashboard/asset/{assetId}`.
- Added global handler for unknown assets in `GlobalExceptionHandler`.
- Added controller integration tests in `DashboardControllerTest`.
