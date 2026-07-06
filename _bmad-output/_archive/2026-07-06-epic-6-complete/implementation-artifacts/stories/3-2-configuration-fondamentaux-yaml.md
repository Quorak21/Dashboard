---
story_id: 3.2
story_key: 3-2-configuration-fondamentaux-yaml
epic: 3
epic_name: Dividendes, fondamentaux et alertes trimestrielles
status: done
baseline_commit: 35ff7e0
created: 2026-06-24
FRs:
  - FR-5
  - FR-6
NFRs:
  - NFR-12
  - NFR-13
dependencies:
  - 1.1 (Registre d'actifs YAML et AssetRegistry)
  - 1.4 (Modèle AssetDto unifié)
---

# Story 3.2: Configuration fondamentaux YAML

Status: done

## Story

En tant que Dokk,
je veux des ratios, NAV, TER et holdings en config avec date de fraîcheur,
afin de déclencher l'alerte trimestrielle.

## Acceptance Criteria

### AC-1: Chargement de la configuration des fondamentaux depuis les fichiers YAML
**Given** un dossier de configuration `{project-root}/backend/src/main/resources/config/fundamentals/`
**When** l'application Spring Boot démarre
**Then** la classe `AssetFundamentalsProperties` charge dynamiquement tous les fichiers `{assetId}.yml` présents dans ce dossier
**And** expose ces configurations sous la forme d'une `Map<String, FundamentalsConfig>` indexée par l'identifiant de l'actif (`assetId` en minuscules).

### AC-2: Structure et types de données de la configuration
**Given** le fichier de configuration de fondamentaux pour un actif (ex. `{project-root}/backend/src/main/resources/config/fundamentals/inveb.yml`)
**When** l'application charge la configuration
**Then** les champs suivants sont typés et validés :
- `asset-id` (String, obligatoire, clé de la map, converti en minuscules)
- `updated-at` (LocalDate, obligatoire)
- `source` (String, obligatoire)
- `metrics` (Map<String, Object>, obligatoire)
- `top-holdings` (Liste d'entrées de holdings, optionnelle, chaque entrée contenant `name` (String) et `weight-percent` (BigDecimal))
- `sector-weights` (Liste d'entrées de sector weights, optionnelle, chaque entrée contenant `sector` (String) et `weight-percent` (BigDecimal))

### AC-3: Fichiers d'exemples fournis
**Given** les données d'Investor AB (inveb) et de BlackRock World Mining (brwm)
**When** l'application démarre
**Then** les fichiers exemples suivants doivent être présents dans le dossier `backend/src/main/resources/config/fundamentals/` :
- `inveb.yml` :
  ```yaml
  asset-id: inveb
  updated-at: "2026-04-15"
  source: "Source: Q1 2026 report"
  metrics:
    trailing-pe: 6.11
    debt-leverage: "1.2%"
    management-cost: "0.09%"
    five-y-nav-cagr: "14.5%"
    five-y-total-return: "112%"
    dry-powder: "~18.2B SEK"
    cash-inflow: "~11.5B SEK"
  top-holdings:
    - { name: "ABB", weight-percent: 16.5 }
    - { name: "Atlas Copco", weight-percent: 15.0 }
    - { name: "SEB", weight-percent: 11.5 }
    - { name: "AstraZeneca", weight-percent: 8.0 }
    - { name: "Mölnlycke", weight-percent: 7.5 }
    - { name: "Epiroc", weight-percent: 5.5 }
    - { name: "EQT AB", weight-percent: 5.0 }
    - { name: "Ericsson", weight-percent: 4.0 }
    - { name: "Nasdaq", weight-percent: 3.5 }
    - { name: "Saab", weight-percent: 3.0 }
  ```
- `brwm.yml` :
  ```yaml
  asset-id: brwm
  updated-at: "2026-03-15"
  source: "Rapport semestriel H2 2025"
  metrics:
    nav-per-share: 612.0
    discount-to-nav: -8.5
    trailing-pe: 12.4
  top-holdings: []
  ```

### AC-4: Intégration dans le DTO unifié `AssetDto`
**Given** un actif dans le registre et ses fondamentaux configurés ou absents
**When** `ConfigurableAssetService.getData(assetId)` ou `syncPrice(assetId)` est appelé
**Then** l'`AssetDto` renvoyé par l'API contient un sous-bloc `fundamentals` (`FundamentalsBlock`)
**And** si la configuration de fondamentaux existe pour cet `assetId`, les champs de `FundamentalsBlock` sont alimentés :
  - `updatedAt` (LocalDate)
  - `source` (String)
  - `stale` (boolean, positionné par défaut à `false`, sera calculé dynamiquement dans la Story 3.4)
  - `metrics` (Map<String, Object>)
  - `topHoldings` (List<HoldingEntry> convertie depuis la config)
  - `sectorWeights` (List<SectorWeight> convertie depuis la config)
**And** si aucun fichier de fondamentaux n'existe pour cet `assetId`, le bloc `fundamentals` est `null` dans l'`AssetDto`.

### AC-5: Tests unitaires
**Given** la classe `AssetFundamentalsPropertiesTest` ou équivalent
**When** la suite de tests JUnit 5 est lancée
**Then** elle valide le chargement correct des fichiers de fondamentaux (cas valide, cas de fichier manquant, cas de données obligatoires manquantes, cas de listes optionnelles null/vides)
**And** la commande `mvn test` à la racine de `/backend` réussit sans erreur.

## Tasks / Subtasks

- [x] Créer les fichiers de configuration YAML (AC-3)
  - [x] Créer le dossier `backend/src/main/resources/config/fundamentals/`
  - [x] Créer le fichier `backend/src/main/resources/config/fundamentals/inveb.yml`
  - [x] Créer le fichier `backend/src/main/resources/config/fundamentals/brwm.yml`
- [x] Implémenter le chargement et la structure des données (AC-1, AC-2)
  - [x] Créer la classe `AssetFundamentalsProperties` dans `com.dokkcorp.dashboard.config.assets`
    - [x] Définir la structure de données imbriquée `FundamentalsConfig`, `HoldingProperties` et `SectorWeightProperties`
    - [x] S'assurer que les getters/setters sont présents pour Spring Boot Binder
  - [x] Créer la classe de configuration `AssetFundamentalsConfiguration` dans `com.dokkcorp.dashboard.config.assets`
    - [x] Utiliser `PathMatchingResourcePatternResolver` et `YamlPropertiesFactoryBean` couplé au `Binder` de Spring Boot
    - [x] Valider la présence des champs obligatoires (`assetId`, `updatedAt`, `source`, `metrics`) et lever des logs d'avertissement en cas d'omission
    - [x] Gérer proprement les cas d'exception et la nullité des ressources retournées par le résolveur de patterns
- [x] Mettre à jour le service et le DTO (AC-4)
  - [x] Injecter `AssetFundamentalsProperties` dans `ConfigurableAssetService`
  - [x] Implémenter la méthode privée `buildFundamentalsBlock(String assetId)`
  - [x] Mettre à jour `ConfigurableAssetService.assembleDto` et `syncPrice` pour instancier `FundamentalsBlock` à partir des propriétés chargées (avec `stale` positionné à `false`)
- [x] Écrire les tests unitaires (AC-5)
  - [x] Créer `AssetFundamentalsPropertiesTest` pour valider le parseur de configuration
  - [x] Mettre à jour `ConfigurableAssetServiceTest` pour mocker la configuration avec/sans fondamentaux
  - [x] Lancer `mvn clean test` depuis `/backend` et s'assurer que tous les tests sont verts

## Dev Notes

### Patterns et Contraintes d'Architecture
- **Pas de rechargement à chaud complexe** : La configuration est chargée au boot. La mise à jour de la configuration des fondamentaux en production se fait par modification du fichier YAML et redémarrage du conteneur (ADR-07).
- **Structure des dossiers** :
  - Config : `backend/src/main/resources/config/fundamentals/{assetId}.yml`
  - Classes : `backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsProperties.java` et `AssetFundamentalsConfiguration.java`
- **Robustesse du Lookup** : Convertir systématiquement les clés en minuscules (`Locale.ROOT`) pour s'assurer que les lookups sur `assetId` ne soient pas sensibles à la casse.

### Prévention des anomalies (Retours d'expérience Story 3.1)
- **Null Safety dans le résolveur de ressources** : Vérifier que le tableau de ressources renvoyé par `resolver.getResources("classpath*:config/fundamentals/*.yml")` n'est pas nul et gérer le cas via un try-catch global pour éviter de faire planter le démarrage de l'application.
- **Validation stricte au démarrage** : Si un fichier YAML est mal structuré ou s'il lui manque un champ obligatoire (`assetId`, `updatedAt`, `source`, `metrics`), logger une alerte claire au niveau WARN et ignorer le fichier concerné plutôt que de planter silencieusement ou d'exposer des données partielles.
- **Conversion sécurisée des sous-listes** : Pour les holdings et sector weights, gérer explicitement la nullité potentielle des listes sources (`getTopHoldings()` ou `getSectorWeights()`) et filtrer les entrées nulles lors du stream de conversion vers `HoldingEntry` et `SectorWeight`.

### Références
- [Contexte du projet](file:///c:/Users/chju/Desktop/Dashboard/docs/project-context.md)
- [Fiche d'Architecture - Section 3.2 (ADR-06)](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/architecture.md#L226)
- [Fiche d'Architecture - Section 3.5 (ADR-09)](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/architecture.md#L279)
- [Spécification de l'Epic 3 dans epics.md](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/epics.md#L359)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (Medium)

### Debug Log References

- Task ID: `93303aa8-479e-48d2-8475-3d0b1ad64419/task-200` (mvn test pass)

### Completion Notes List

- Création des fichiers YAML exemples pour les fondamentaux : `config/fundamentals/inveb.yml` et `brwm.yml`.
- Création des classes de chargement et de validation Spring Boot Properties/Configuration : `AssetFundamentalsProperties.java` et `AssetFundamentalsConfiguration.java`.
- Intégration de `AssetFundamentalsProperties` dans `ConfigurableAssetService.java` et implémentation de `buildFundamentalsBlock` pour alimenter le DTO.
- Écriture des tests unitaires `AssetFundamentalsPropertiesTest.java` et extension de `ConfigurableAssetServiceTest.java`.
- Tous les tests ont été exécutés avec succès via Maven (92 tests passés, 0 échec).

### File List

- `backend/src/main/resources/config/fundamentals/inveb.yml`
- `backend/src/main/resources/config/fundamentals/brwm.yml`
- `backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsProperties.java`
- `backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsConfiguration.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java`
- `backend/src/test/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsPropertiesTest.java`
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetServiceTest.java`

### Review Findings

- [x] [Review][Patch] Character Encoding Corruption in `inveb.yml` [backend/src/main/resources/config/fundamentals/inveb.yml]
- [x] [Review][Patch] Ineffective Validation of Mandatory `metrics` Field [backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsConfiguration.java:230]
- [x] [Review][Patch] Incomplete and Ineffective Unit Test Coverage for Validation Logic [backend/src/test/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsPropertiesTest.java]
- [x] [Review][Patch] Performance bug on empty snapshot history [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:553]
- [x] [Review][Patch] Encapsulation leak of mutable lists and maps in DTO assembly [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:581]
- [x] [Review][Patch] Unchecked null values in DTO conversion and Price synchronization [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java]
- [x] [Review][Patch] Asset ID leading/trailing whitespace in lookup keys [backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsConfiguration.java:60]
- [x] [Review][Patch] Duplicate assetIds across multiple configuration files overwrites previous entries silently [backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsConfiguration.java:61]
- [x] [Review][Defer] Synchronous DB query on read requests [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:360] — deferred, pre-existing
- [x] [Review][Defer] Thread-safety and Lock Replacement Risks in `synchronizeCachesWithRegistry` [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java] — deferred, pre-existing
- [x] [Review][Defer] Unit tests coupled to production files [backend/src/test/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsPropertiesTest.java] — deferred, pre-existing
- [x] [Review][Defer] Redundant AtomicReference wrapper in cache [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java] — deferred, pre-existing
- [x] [Review][Defer] Stale Cache Age calculation bug [backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java:443] — deferred, pre-existing

