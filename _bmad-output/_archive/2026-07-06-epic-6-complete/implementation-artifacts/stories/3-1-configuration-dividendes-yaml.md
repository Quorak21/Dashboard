---
story_id: 3.1
story_key: 3-1-configuration-dividendes-yaml
epic: 3
epic_name: Dividendes, fondamentaux et alertes trimestrielles
status: done
baseline_commit: 35ff7e0
created: 2026-06-24
FRs:
  - FR-4
  - FR-6
NFRs:
  - NFR-12
  - NFR-13
dependencies:
  - 1.1 (Registre d'actifs YAML et AssetRegistry)
  - 1.4 (Modèle AssetDto unifié)
---

# Story 3.1: Configuration dividendes YAML

Status: done

## Story

En tant que Dokk,
je veux maintenir l'historique dividendes en fichiers YAML versionnés,
afin de mettre à jour sans redéployer du code.

## Acceptance Criteria

### AC-1: Chargement de la configuration des dividendes depuis les fichiers YAML
**Given** un dossier de configuration `{project-root}/backend/src/main/resources/config/dividends/`
**When** l'application Spring Boot démarre
**Then** la classe `AssetDividendsProperties` charge dynamiquement tous les fichiers `{assetId}.yml` présents dans ce dossier
**And** expose ces configurations sous la forme d'une `Map<String, DividendsConfig>` indexée par l'identifiant de l'actif (`assetId` en minuscules).

### AC-2: Structure et types de données de la configuration
**Given** le fichier de configuration de dividendes pour un actif (ex. `{project-root}/backend/src/main/resources/config/dividends/inveb.yml`)
**When** l'application charge la configuration
**Then** les champs suivants sont typés et validés :
- `asset-id` (String, obligatoire, clé de la map, converti en minuscules)
- `forward-dividend` (BigDecimal, obligatoire)
- `forward-dividend-currency` (String, obligatoire)
- `frequency` (String, obligatoire, e.g. "annual", "quarterly", "monthly")
- `avg-dividend-growth-10y` (BigDecimal, optionnel, nullable)
- `history` (Liste d'entrées d'historique de dividendes, chaque entrée contenant `year` (int), `amount` (BigDecimal), et `currency` (String))

### AC-3: Fichier d'exemple `inveb.yml` fourni
**Given** les données de dividendes d'Investor AB (InveB)
**When** l'application est démarrée
**Then** le fichier `{project-root}/backend/src/main/resources/config/dividends/inveb.yml` doit être présent et configuré avec les valeurs suivantes :
```yaml
asset-id: inveb
forward-dividend: 6.00
forward-dividend-currency: SEK
frequency: annual
avg-dividend-growth-10y: 8.2
history:
  - { year: 2024, amount: 6.00, currency: SEK }
  - { year: 2023, amount: 5.50, currency: SEK }
```

### AC-4: Intégration dans le DTO unifié `AssetDto`
**Given** un actif dans le registre et ses dividendes configurés ou absents
**When** `ConfigurableAssetService.getData(assetId)` ou `syncPrice(assetId)` est appelé
**Then** l'`AssetDto` renvoyé par l'API contient un sous-bloc `dividends` (`DividendsBlock`)
**And** si la configuration de dividendes existe pour cet `assetId`, les champs de `DividendsBlock` sont alimentés :
  - `forwardDividend` (BigDecimal)
  - `forwardDividendCurrency` (String)
  - `frequency` (String)
  - `avgDividendGrowth10Y` (BigDecimal)
  - `history` (List<DividendHistoryEntry>)
  - `estimatedYield` est positionné à `null` (sera calculé dans la Story 3.3)
**And** si aucun fichier de dividendes n'existe pour cet `assetId`, le bloc `dividends` est `null` dans l'`AssetDto`.

### AC-5: Tests unitaires
**Given** la classe `AssetDividendsPropertiesTest` ou équivalent
**When** la suite de tests JUnit 5 est lancée
**Then** elle valide le chargement correct des fichiers de dividendes (cas valide, cas de fichier manquant, cas de données optionnelles manquantes)
**And** la commande `mvn test` à la racine de `/backend` réussit sans erreur.

## Tasks / Subtasks

- [x] Créer les fichiers de configuration YAML (AC: #2, #3)
  - [x] Créer le dossier `backend/src/main/resources/config/dividends/`
  - [x] Créer le fichier `backend/src/main/resources/config/dividends/inveb.yml`
- [x] Implémenter le chargement et la structure des données (AC: #1, #2)
  - [x] Créer la classe `AssetDividendsProperties` avec la structure imbriquée `DividendsConfig` (dans le package `com.dokkcorp.dashboard.config.assets`)
  - [x] Créer la classe de configuration `AssetDividendsConfiguration` pour charger dynamiquement tous les fichiers `classpath*:config/dividends/*.yml`
  - [x] Utiliser `PathMatchingResourcePatternResolver` et `YamlPropertiesFactoryBean` couplé au `Binder` de Spring Boot pour mapper dynamiquement les fichiers sans les surcharger manuellement
- [x] Mettre à jour le service et le DTO (AC: #4)
  - [x] Injecter `AssetDividendsProperties` dans `ConfigurableAssetService`
  - [x] Mettre à jour `ConfigurableAssetService.assembleDto` et `syncPrice` pour instancier `DividendsBlock` à partir des propriétés chargées (avec `estimatedYield` à `null` pour le moment)
- [x] Écrire les tests unitaires (AC: #5)
  - [x] Créer `AssetDividendsPropertiesTest` pour valider le parseur de configuration et la Map
  - [x] Mettre à jour `ConfigurableAssetServiceTest` pour mock le comportement avec/sans configuration de dividendes
  - [x] Lancer `mvn clean test` depuis `/backend` et vérifier le statut au vert

### Review Findings

- [x] [Review][Patch] Thread-Safety and Lock Invalidation in Cache Synchronization [ConfigurableAssetService.java:83]
- [x] [Review][Patch] Thundering Herd/Concurrent DB queries in refreshHistory [ConfigurableAssetService.java:272]
- [x] [Review][Patch] Uncached DB queries in read path (getData) [ConfigurableAssetService.java:106]
- [x] [Review][Patch] Inconsistent French warning logs [ConfigurableAssetService.java:177]
- [x] [Review][Patch] Cache Metadata Desynchronization in DTO Assembly [ConfigurableAssetService.java:201]
- [x] [Review][Patch] Missing validation for mandatory fields in dividend config files [AssetDividendsProperties.java:21]
- [x] [Review][Patch] Incomplete config loading unit tests [AssetDividendsPropertiesTest.java:15]
- [x] [Review][Patch] Missing assertions for null dividends when config is absent [ConfigurableAssetServiceTest.java:432]
- [x] [Review][Patch] NullPointerException on Null Asset Provider [ConfigurableAssetService.java:121]
- [x] [Review][Patch] Uncaught database write exceptions in daily price persistence [ConfigurableAssetService.java:127]
- [x] [Review][Patch] Potential NullPointerException on scan resources [AssetDividendsConfiguration.java:30]
- [x] [Review][Patch] Potential NullPointerException if MarketHours zone is null [ConfigurableAssetService.java:237]
- [x] [Review][Patch] Potential NullPointerException on snapshot list size check [ConfigurableAssetService.java:281]
- [x] [Review][Patch] Potential NullPointerException on null history entries [ConfigurableAssetService.java:309]
- [x] [Review][Patch] Fragile timestamp handling in live series [ConfigurableAssetService.java:248]

## Dev Notes

### Patterns et Contraintes d'Architecture
- **Pas de rechargement à chaud complexe** : La configuration est chargée au boot. La mise à jour de la configuration de dividendes en production se fait par modification du fichier YAML et redémarrage du conteneur (ADR-07).
- **Structure des dossiers** :
  - Config : `backend/src/main/resources/config/dividends/{assetId}.yml`
  - Classes : `backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetDividendsProperties.java` et `AssetDividendsConfiguration.java`
  - Les types métier pour le DTO (`DividendsBlock` et `DividendHistoryEntry`) existent déjà dans `com.dokkcorp.dashboard.features.assets.model`.
- **Nommage** : Convertir systématiquement les clés en minuscules pour s'assurer que les lookups sur `assetId` ne soient pas sensibles à la casse.

### Chargement dynamique des YAML multiples
Pour charger dynamiquement un ensemble de fichiers YAML indépendants dans une structure de Map, utilisez la méthode suivante dans `AssetDividendsConfiguration` :
```java
@Configuration
public class AssetDividendsConfiguration {

    @Bean
    public AssetDividendsProperties assetDividendsProperties() throws IOException {
        AssetDividendsProperties properties = new AssetDividendsProperties();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:config/dividends/*.yml");
        
        for (Resource resource : resources) {
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(resource);
            Properties props = factory.getObject();
            if (props != null) {
                MapConfigurationPropertySource source = new MapConfigurationPropertySource((Map) props);
                Binder binder = new Binder(source);
                DividendsConfig config = binder.bind("", Bindable.of(DividendsConfig.class)).orElse(null);
                if (config != null && config.getAssetId() != null) {
                    properties.getDividends().put(config.getAssetId().toLowerCase(Locale.ROOT), config);
                }
            }
        }
        return properties;
    }
}
```
Cette approche évite d'avoir à déclarer les clés dans `application.yml` ou d'avoir des conflits de propriétés globales.

### Références
- [Contexte du projet](file:///c:/Users/chju/Desktop/Dashboard/docs/project-context.md)
- [Fiche d'Architecture - Section 6.2](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/architecture.md#L627)
- [Fiche d'Architecture - Section 2.3](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/architecture.md#L226)

## Dev Agent Record

### Agent Model Used

Gemini 3.5 (Medium)

### Debug Log References

### Completion Notes List

- Chargement dynamique des fichiers de dividendes YAML depuis `config/dividends/*.yml`.
- Liaison et validation avec Spring Boot Binder et YamlPropertiesFactoryBean.
- Injection d'AssetDividendsProperties dans ConfigurableAssetService et exposition du bloc dividends.
- Fichier exemple config/dividends/inveb.yml créé pour Investor AB.
- Tests unitaires et d'intégration validés avec succès (86 tests au total, 0 erreur).

### File List

- `backend/src/main/resources/config/dividends/inveb.yml`
- `backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetDividendsProperties.java`
- `backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetDividendsConfiguration.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java`
- `backend/src/test/java/com/dokkcorp/dashboard/config/assets/AssetDividendsPropertiesTest.java`
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetServiceTest.java`

### Change Log

- 2026-06-24 — Implémentation initiale, configuration et tests validés.
