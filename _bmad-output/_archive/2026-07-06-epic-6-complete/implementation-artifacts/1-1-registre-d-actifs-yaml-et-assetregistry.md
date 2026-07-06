---
baseline_commit: 35ff7e019c6faf63eee0585a9bda731d54513a27
---

# Story 1.1: Registre d'actifs YAML et AssetRegistry

Status: review

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

- [x] Task 1: Créer `config/assets-registry.yml` avec entrée `inveb` (AC: #1)
  - [x] Définir provider fmp, symbol INVE-B.ST, db-symbol INVE-B, type STOCK, currency SEK
  - [x] Configurer market-hours Europe/Stockholm 09:00–17:35 et sync 15/0
- [x] Task 2: Implémenter `AssetRegistryProperties` + binding Spring (AC: #1, #2)
  - [x] `@ConfigurationProperties(prefix = "app.assets-registry")`
  - [x] Import YAML via `spring.config.import` dans `application.yml`
- [x] Task 3: Implémenter `AssetRegistry` et validation au démarrage (AC: #1, #2)
  - [x] Interface `AssetRegistry` avec `findById`, `all`, `byProvider`
  - [x] `DefaultAssetRegistry` + `AssetRegistryMapper` avec messages d'erreur explicites
- [x] Task 4: Tests (AC: #3)
  - [x] `AssetRegistryTest` charge depuis YAML de test et vérifie `inveb`
  - [x] Test validation market-hours invalide

## Dev Notes

- ADR-02 : `backend/src/main/resources/config/assets-registry.yml` via `@ConfigurationProperties(prefix = "app.assets-registry")`
- `db-symbol` distinct de `symbol` pour rétrocompat BD InveB (ADR-03)
- Premier usage de `@ConfigurationProperties` dans le projet (avant tout en `@Value`)
- HYPE reste hors registre v1 — pas de régression

### Project Structure Notes

- `config/assets/AssetRegistryProperties.java`, `AssetRegistryConfiguration.java`
- `features/assets/AssetRegistry.java`, `DefaultAssetRegistry.java`, `AssetRegistryMapper.java`
- `features/assets/model/*` — records immutables domaine

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.1]
- [Source: _bmad-output/planning-artifacts/architecture.md#ADR-02]

## Dev Agent Record

### Agent Model Used

claude-4.6-sonnet-medium-thinking

### Debug Log References

- Compilation fix: removed cross-package import of package-private `AssetRegistryMapper`

### Completion Notes List

- Registre YAML `inveb` chargé au démarrage via `spring.config.import`
- `AssetRegistry.findById("inveb")` retourne définition typée (provider, symboles, market-hours, sync, currency)
- Validation startup avec `IllegalStateException` messages explicites (champs manquants, zone invalide, open≥close, sync invalide, duplicate id)
- `AssetRegistryTest` : chargement YAML + rejet market-hours invalide
- Suite Maven complète : 0 échec (`.\mvnw.cmd test`)

### File List

- backend/src/main/resources/config/assets-registry.yml
- backend/src/main/resources/application.yml
- backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetRegistryProperties.java
- backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetRegistryConfiguration.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/AssetRegistry.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/DefaultAssetRegistry.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/AssetRegistryMapper.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetDefinition.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetProvider.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetType.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/MarketHours.java
- backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/SyncConfig.java
- backend/src/test/resources/config/assets-registry.yml
- backend/src/test/resources/application.yml
- backend/src/test/java/com/dokkcorp/dashboard/features/assets/AssetRegistryTest.java

## Change Log

- 2026-06-17: Story 1.1 implémentée — registre YAML multi-actifs, AssetRegistry, validation startup, tests
