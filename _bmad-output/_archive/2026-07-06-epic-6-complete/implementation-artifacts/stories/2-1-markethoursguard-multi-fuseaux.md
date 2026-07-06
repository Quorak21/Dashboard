---
baseline_commit: 35ff7e019c6faf63eee0585a9bda731d54513a27
---

# Story 2.1: MarketHoursGuard multi-fuseaux

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

En tant que Dokk,
je veux que la sync et l'écriture intraday respectent les heures de chaque bourse,
afin de ne pas polluer AssetDaily la nuit ou le week-end.

## Acceptance Criteria

1. **Given** une entrée de registre avec `market-hours.zone`, `open` et `close` configurés (ex. Europe/Stockholm, open=09:00, close=17:35).
2. **When** `MarketHoursGuard.isOpen(asset)` est évalué un week-end (samedi/dimanche) ou en dehors de la plage horaire `[open, close]`.
3. **Then** la méthode doit retourner `false`, et la méthode `status(asset)` doit retourner `MarketStatus.CLOSED`.
4. **And** le DTO d'un actif (`AssetDto`) doit inclure le champ `marketStatus` avec la valeur `CLOSED` lorsque le marché est fermé, et `OPEN` lorsque le marché est ouvert.
5. **And** les tests unitaires couvrent de manière rigoureuse les fuseaux horaires et configurations de bourses suivantes :
   - `Europe/Stockholm` (ex: `inveb` / Investor AB, open=09:00, close=17:35)
   - `Europe/London` (ex: `brwm` / BlackRock, open=08:00, close=16:30)
   - `America/New_York` (ex: `o` / Realty Income, open=09:30, close=16:00)
   - `Europe/Zurich` (ex: `chdiv` / CHDIV, open=09:00, close=17:30)

## Tasks / Subtasks

- [x] Vérifier et s'assurer que `MarketHoursGuard.java` respecte pleinement les critères d'acceptation (AC: #1, #2, #3)
  - [x] Inspecter le fichier [MarketHoursGuard.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/main/java/com/dokkcorp/dashboard/features/assets/MarketHoursGuard.java).
  - [x] Confirmer que la logique gère correctement les fuseaux horaires via `clock.withZone(hours.zone())` et les limites de temps via `isBefore`/`isAfter`.
- [x] Compléter et exécuter les tests unitaires de `MarketHoursGuard` (AC: #5)
  - [x] Ouvrir le fichier de test existant [MarketHoursGuardTest.java](file:///c:/Users/chju/Desktop/Dashboard/backend/src/test/java/com/dokkcorp/dashboard/features/assets/MarketHoursGuardTest.java).
  - [x] Ajouter des cas de test complets couvrant `Europe/London` (par exemple un lundi à 07:59 CLOSED, 08:01 OPEN, 16:29 OPEN, 16:31 CLOSED).
  - [x] Ajouter des cas de test complets couvrant `America/New_York` (par exemple un mercredi à 09:29 CLOSED, 09:31 OPEN, 15:59 OPEN, 16:01 CLOSED).
  - [x] Ajouter des cas de test complets couvrant `Europe/Zurich` (par exemple un vendredi à 08:59 CLOSED, 09:01 OPEN, 17:29 OPEN, 17:31 CLOSED).
  - [x] S'assurer que le mock du `Clock` et du fuseau horaire (`fixedClock` ou similaire) est correctement configuré et testé pour chaque zone.
- [x] Valider l'intégration avec `ConfigurableAssetService` et le modèle `AssetDto` (AC: #4)
  - [x] Inspecter comment `ConfigurableAssetService.java` utilise `MarketHoursGuard.status(asset)` pour renseigner le statut de marché dans `AssetDto`.
  - [x] S'assurer que les tests existants dans `ConfigurableAssetServiceTest.java` continuent de passer.
- [x] Exécuter tous les tests du backend (AC: #5)
  - [x] Lancer la commande `mvn test` dans le dossier `backend/` et s'assurer que tous les tests passent avec succès.

### Review Findings

- [x] [Review][Decision] `docs/` entier ignoré dans `.gitignore` — Décision : docs/ est local BMad, intentionnellement ignoré. Dismissed.
- [x] [Review][Decision] `AssetDto` Java sans miroir TypeScript — Décision : différé à Story 4-1 (Epic 4 dédié à la migration front). Raison : créer le modèle sans composant cible serait prématuré.
- [x] [Review][Patch] `toLowerCase()` sans `Locale.ROOT` dans `DefaultAssetRegistry.findById` [DefaultAssetRegistry.java:33]
- [x] [Review][Patch] NPE si `asset.marketHours()` est `null` dans `MarketHoursGuard.isOpen/status` [MarketHoursGuard.java:28]
- [x] [Review][Patch] `FmpPriceProvider.fetch` : accès `[0]` sans vérification de la longueur du tableau renvoyé par l'API FMP [FmpPriceProvider.java]
- [x] [Review][Patch] Collision de routes : `/{assetId}` peut entrer en conflit avec `/hype`, `/inveb` et futurs endpoints [DashboardController.java:38]
- [x] [Review][Patch] `snap.getPrice()` peut être `null` dans `refreshHistory`, insérant des `null` dans `historyPrices` [ConfigurableAssetService.java:247]
- [x] [Review][Patch] Version WireMock `3.13.1` codée en dur dans `pom.xml` — extraite dans `<properties>` [pom.xml:44]
- [x] [Review][Defer] Race condition non atomique sur `historyPrices`/`historyDays` dans `refreshHistory` [ConfigurableAssetService.java:254] — deferred, pré-existant / patterns établis
- [x] [Review][Defer] Interception globale de `Exception` dans `syncPrice` masque les bugs internes [ConfigurableAssetService.java:140] — deferred, pré-existant
- [x] [Review][Defer] `System.currentTimeMillis()` rend les tests d'expiration de l'historique non déterministes [ConfigurableAssetService.java:239] — deferred, refactoring Clock à planifier
- [x] [Review][Defer] Duplication de `db-symbol` entre actifs non détectée dans `DefaultAssetRegistry` [DefaultAssetRegistry.java:12] — deferred, faible risque immédiat
- [x] [Review][Defer] Instabilité de verrous sur rechargement du registre dans `synchronizeCachesWithRegistry` [ConfigurableAssetService.java:71] — deferred, rechargement à chaud non implémenté
- [x] [Review][Defer] `getData` renvoie un DTO erreur au démarrage à froid avant premier run du scheduler [ConfigurableAssetService.java:87] — deferred, UX acceptable pour l'instant
- [x] [Review][Defer] Actifs `brwm`, `o`, `chdiv` absents du registre de production `assets-registry.yml` — deferred, prévu dans Epics ultérieurs
- [x] [Review][Defer] Package `model` (singulier) vs convention docs `models` (pluriel) — deferred, incohérence mineure

## Dev Notes

- **Code Existant** : `MarketHoursGuard.java` contient déjà une implémentation de base avec `isOpen()` et `status()`. Le travail principal consiste à ajouter la couverture de tests manquante pour les autres bourses (Londres, New York, Zurich) et valider le bon fonctionnement général.
- **Modèle de Données** :
  - L'entité `MarketHours` dans `MarketHours.java` encapsule `ZoneId zone`, `LocalTime open` et `LocalTime close`.
  - `AssetDefinition` contient une référence à `MarketHours`.
- **Règles Temporelles** :
  - Le calcul du statut ouvert/fermé utilise `ZonedDateTime.now(clock.withZone(hours.zone()))` qui applique le bon décalage horaire indépendamment de l'heure locale de la machine exécutant le backend.
  - S'assurer que les objets `Clock` figés utilisés dans les tests sont créés avec le fuseau horaire cible pour éviter tout décalage d'interprétation lors des assertions.

### Project Structure Notes

- **Fichiers impliqués** :
  - `backend/src/main/java/com/dokkcorp/dashboard/features/assets/MarketHoursGuard.java` (Lecture / Validation)
  - `backend/src/test/java/com/dokkcorp/dashboard/features/assets/MarketHoursGuardTest.java` (Modification)
  - `backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java` (Lecture / Validation)

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-2.1](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/epics.md#L260-L276)
- [Source: docs/project-context.md](file:///c:/Users/chju/Desktop/Dashboard/docs/project-context.md)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium)

### Debug Log References

- Maven Test Run Task ID: `d6032395-94a8-4dc1-a6b7-b6c9eb24a02b/task-128`

### Completion Notes List

- Vérifié l'implémentation de `MarketHoursGuard.java` (gestion week-end et limites horaires).
- Étendu `MarketHoursGuardTest.java` pour tester les bourses de Stockholm, Londres, New York et Zurich.
- Validé l'intégration de `MarketStatus` dans `AssetDto` via `ConfigurableAssetService`.
- Exécuté avec succès tous les tests du backend (64/64 au vert).

### File List

- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/MarketHoursGuardTest.java`

