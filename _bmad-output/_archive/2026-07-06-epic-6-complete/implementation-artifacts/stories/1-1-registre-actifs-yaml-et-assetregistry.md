# Story 1.1: Registre d'actifs YAML et AssetRegistry

Status: done

## Story

En tant que Dokk,
je veux déclarer les actifs dans un fichier `assets-registry.yml`,
afin d'ajouter un futur titre sans créer un nouveau service Java.

## Acceptance Criteria

1. **Given** le fichier `config/assets-registry.yml` avec au minimum l'entrée `inveb` (provider fmp, symbol INVE-B.ST, db-symbol INVE-B)
   **When** l'application Spring Boot démarre
   **Then** `AssetRegistryProperties` charge toutes les entrées et `AssetRegistry.findById("inveb")` retourne la définition complète

2. **And** les champs `market-hours`, `sync.interval-minutes`, `sync.offset-minutes` et `currency` sont typés et validés au démarrage (erreur claire si entrée invalide)

3. **And** un test unitaire `AssetRegistryTest` vérifie le chargement depuis un YAML de test

## Tasks / Subtasks

- [x] Créer `config/assets-registry.yml` avec entrée `inveb` (AC: #1)
- [x] Implémenter `AssetRegistryProperties` + `AssetRegistryConfiguration` (AC: #1)
- [x] Implémenter `AssetRegistry`, `DefaultAssetRegistry`, `AssetRegistryMapper` et modèles (AC: #1, #2)
- [x] Valider `market-hours`, `sync` et `currency` au démarrage (AC: #2)
- [x] Écrire `AssetRegistryTest` — happy path + cas négatifs (AC: #3)

### Review Findings

- [x] [Review][Patch] Tests négatifs `sync` absents — ajoutés
- [x] [Review][Patch] Tests négatifs `currency` absents — ajoutés
- [x] [Review][Patch] Erreurs `provider`/`type` sans contexte asset — enveloppées dans le mapper
- [x] [Review][Patch] `getEntries()` expose une liste mutable — `List.copyOf`
- [x] [Review][Patch] Élément `null` dans la liste → NPE — garde ajoutée
- [x] [Review][Patch] Imports inutilisés dans `AssetRegistry.java` — supprimés
- [x] [Review][Decision] Validation `currency` — `requireText` (non vide), pas ISO 4217
- [x] [Review][Decision] Normalisation des `id` en lowercase — appliquée au chargement et `findById`

## Dev Notes

- FRs : FR-1 · NFRs : NFR-14
- Source : `_bmad-output/planning-artifacts/epics.md` (Story 1.1)
- Les `id` sont normalisés en lowercase (`Locale.ROOT`) à l'import YAML et dans `findById`
- `currency` validé par présence (non vide), cohérent avec le reste du projet

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-1.1]
- [Source: _bmad-output/planning-artifacts/architecture.md]

## Dev Agent Record

### Agent Model Used

claude (Cursor Agent)

### Completion Notes List

- Registre YAML opérationnel avec entrée `inveb`
- Validation au démarrage via `AssetRegistryMapper` avec messages contextualisés par `assetId`
- 8 tests dans `AssetRegistryTest` (happy path, casse insensible, doublons, sync, currency, provider)
- Code review BMad complétée ; patches appliqués

### File List

- `backend/src/main/resources/config/assets-registry.yml`
- `backend/src/main/resources/application.yml` (import YAML)
- `backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetRegistryProperties.java`
- `backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetRegistryConfiguration.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/AssetRegistry.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/DefaultAssetRegistry.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/AssetRegistryMapper.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetDefinition.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetProvider.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetType.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/MarketHours.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/SyncConfig.java`
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/AssetRegistryTest.java`
- `backend/src/test/resources/config/assets-registry.yml`
- `backend/src/test/resources/application.yml`

## Change Log

- 2026-06-17 — Implémentation initiale Story 1.1
- 2026-06-17 — Code review : patches appliqués, statut `done`
