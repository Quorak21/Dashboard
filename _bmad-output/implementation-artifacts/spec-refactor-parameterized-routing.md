---
title: 'Rendu dynamique du dashboard et suppression du hardcoding des actifs'
type: 'refactor'
created: '2026-07-06'
status: 'done'
baseline_commit: '3fd3f954048ea37ef9f7aab809eab36b940af087'
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Le tableau de bord (`dashboard.html` / `dashboard.ts`) affiche et souscrit à des actifs codés en dur (`inveb`, `brwm`, `o`). L'ajout ou le retrait d'un actif dans la configuration nécessite de modifier manuellement le code du composant et du template HTML du dashboard, ce qui est source d'erreurs de duplication et de maintenance.

**Approach:** Exposer un nouvel endpoint backend `/api/dashboard/assets` retournant la liste de tous les actifs du registre. Côté frontend, récupérer cette liste au démarrage du dashboard via `DashboardApiService`, initialiser les abonnements RxJS dynamiquement pour chaque actif retourné, et utiliser le control flow `@for` d'Angular pour rendre les cartes de dashboard à la volée.

## Boundaries & Constraints

**Always:**
- Utiliser la syntaxe standalone d'Angular 21.
- Conserver le typage TypeScript strict (strict: true), sans utiliser de type `any`.
- Libérer proprement les abonnements RxJS via `takeUntilDestroyed` ou en appelant explicitement `unsubscribe()`.
- Utiliser le control flow `@for` d'Angular avec une clause `track` unique.

**Ask First:**
- (Aucun)

**Never:**
- Ne pas modifier l'affichage ni la logique propre à l'actif HYPE (qui possède des cartes et des graphiques spécifiques sous la catégorie Cryptocurrencies).
- Ne pas conserver de tableau d'identifiants d'actifs en dur dans le fichier `dashboard.ts` (comme `['inveb', 'brwm', 'o']`).

</frozen-after-approval>

## Code Map

- [RegisteredAssetDto.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/RegisteredAssetDto.java) -- [NEW] Nouveau record Java représentant un actif enregistré (id, displayName, type, currency).
- [ConfigurableAssetService.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java) -- Service backend à modifier pour exposer la liste des actifs mappés du registre.
- [DashboardController.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/java/com/dokkcorp/dashboard/controller/DashboardController.java) -- Contrôleur backend à modifier pour ajouter l'endpoint GET `/api/dashboard/assets`.
- [DashboardControllerTest.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/test/java/com/dokkcorp/dashboard/controller/DashboardControllerTest.java) -- Tests du contrôleur backend à mettre à jour pour tester le nouvel endpoint.
- [asset.dto.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/core/models/asset.dto.ts) -- Fichier de modèles TypeScript à enrichir avec l'interface `RegisteredAssetDto`.
- [index.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/core/models/index.ts) -- Index de modèles pour exporter le nouveau type `RegisteredAssetDto`.
- [dashboard-api.service.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/core/services/dashboard-api.service.ts) -- Service API à modifier pour ajouter l'appel `getRegisteredAssets()`.
- [dashboard-api.service.spec.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/core/services/dashboard-api.service.spec.ts) -- Tests unitaires du service API pour couvrir la nouvelle méthode.
- [dashboard.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/features/dashboard/dashboard.ts) -- Composant du tableau de bord à modifier pour charger dynamiquement les actifs et gérer les subscriptions dynamiques.
- [dashboard.html](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/features/dashboard/dashboard.html) -- Template HTML à modifier pour rendre dynamiquement les cartes avec `@for`.
- [dashboard.spec.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/features/dashboard/dashboard.spec.ts) -- Tests unitaires du dashboard à adapter avec le mock de la liste dynamique des actifs.

## Tasks & Acceptance

**Execution:**
- [x] [RegisteredAssetDto.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/RegisteredAssetDto.java) -- Créer le record `RegisteredAssetDto` avec les champs `id`, `displayName`, `type`, et `currency`.
- [x] [ConfigurableAssetService.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java) -- Ajouter la méthode `getRegisteredAssets()` qui mappe `assetRegistry.all()` vers `List<RegisteredAssetDto>`.
- [x] [DashboardController.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/java/com/dokkcorp/dashboard/controller/DashboardController.java) -- Exposer `/api/dashboard/assets` en retournant `configurableAssetService.getRegisteredAssets()`.
- [x] [DashboardControllerTest.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/test/java/com/dokkcorp/dashboard/controller/DashboardControllerTest.java) -- Écrire un test unitaire validant le comportement et le format JSON retourné par `/api/dashboard/assets`.
- [x] [asset.dto.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/core/models/asset.dto.ts) -- Ajouter et exporter l'interface `RegisteredAssetDto`.
- [x] [index.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/core/models/index.ts) -- Exposer `RegisteredAssetDto` dans les exports.
- [x] [dashboard-api.service.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/core/services/dashboard-api.service.ts) -- Ajouter la méthode `getRegisteredAssets()`.
- [x] [dashboard-api.service.spec.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/core/services/dashboard-api.service.spec.ts) -- Ajouter le test pour `getRegisteredAssets()`.
- [x] [dashboard.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/features/dashboard/dashboard.ts) -- Supprimer le tableau d'IDs codés en dur. Appeler `getRegisteredAssets()` au démarrage pour initialiser `registeredAssets`. Mettre à jour `refresh()` pour s'abonner dynamiquement aux actifs retournés.
- [x] [dashboard.html](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/features/dashboard/dashboard.html) -- Supprimer les balises `<app-asset-dashboard-card>` codées en dur pour `inveb`, `brwm` et `o`. Remplacer par un bloc `@for` sur `registeredAssets()`.
- [x] [dashboard.spec.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/features/dashboard/dashboard.spec.ts) -- Mocker `getRegisteredAssets()` pour renvoyer la liste d'actifs de test et ajuster les assertions.

**Acceptance Criteria:**
- Given le dashboard principal, when la page est chargée, then les actifs de type stock (inveb, brwm, o) sont chargés dynamiquement depuis le backend.
- Given le chargement du dashboard, when la réponse du serveur contient la liste des actifs, then les cartes correspondantes s'affichent correctement avec le bon symbole de devise (`SEK` pour Investor, `£` pour World Mining, `$` pour Realty).
- Given le dashboard principal, when un actif est ajouté au registre backend sans modification du code Angular, then cet actif s'affiche automatiquement sur le dashboard après rechargement.

## Verification

**Commands:**
- `.\mvnw.cmd clean test` dans `backend/` -- expected: Tous les tests backend passent.
- `npm run test` dans `frontend/` -- expected: Tous les tests unitaires Angular passent.

## Suggested Review Order

**Contrat du backend — endpoint + DTO**

- Record Java minimal exposant exactement les 4 champs attendus.
  [`RegisteredAssetDto.java:1`](../../../backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/RegisteredAssetDto.java#L1)

- Mapping depuis `assetRegistry.all()` sans mutation ni copie défensive inutile.
  [`ConfigurableAssetService.java:455`](../../../backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java#L455)

- Endpoint GET `/api/dashboard/assets` délégant directement au service.
  [`DashboardController.java:51`](../../../backend/src/main/java/com/dokkcorp/dashboard/controller/DashboardController.java#L51)

**Contrat du frontend — interface + service HTTP**

- Interface TypeScript miroir du record Java.
  [`asset.dto.ts:71`](../../../frontend/src/app/core/models/asset.dto.ts#L71)

- Méthode `getRegisteredAssets()` avec catchError uniforme.
  [`dashboard-api.service.ts:51`](../../../frontend/src/app/core/services/dashboard-api.service.ts#L51)

**Composant Dashboard — orchestration dynamique**

- Signal `registeredAssets`, `loadRegisteredAssets()` avec `takeUntilDestroyed`, et `refresh()` dynamique.
  [`dashboard.ts:23`](../../../frontend/src/app/features/dashboard/dashboard.ts#L23)

- `@for` sur `registeredAssets()` : suppression de toute balise statique.
  [`dashboard.html:42`](../../../frontend/src/app/features/dashboard/dashboard.html#L42)

**Tests**

- Test du nouvel endpoint backend avec mock Mockito.
  [`DashboardControllerTest.java:162`](../../../backend/src/test/java/com/dokkcorp/dashboard/controller/DashboardControllerTest.java#L162)

- Tests service HTTP Angular (succès + erreur).
  [`dashboard-api.service.spec.ts:153`](../../../frontend/src/app/core/services/dashboard-api.service.spec.ts#L153)

- Tests dashboard avec `getRegisteredAssets` mocké.
  [`dashboard.spec.ts:56`](../../../frontend/src/app/features/dashboard/dashboard.spec.ts#L56)
