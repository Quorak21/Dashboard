---
baseline_commit: 35ff7e0
---

# Story 1.2: Interface PriceProvider et FmpPriceProvider

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

En tant que Dokk,
je veux un adaptateur FMP réutilisable pour tous les actifs du registre,
afin de centraliser les appels `/stable/profile`.

## Acceptance Criteria

1. **Given** `FMPClient` existant et une entrée registre `provider: fmp`
   **When** `FmpPriceProvider.fetch(asset)` est appelé
   **Then** il retourne un `PriceQuote` avec `price`, `currency`, `marketCap`, `changePercent24h`, `volume` et `fetchedAt`

2. **And** les erreurs provider passent par `ExternalCallExecutor` (NFR-7)

3. **And** `PriceProviderRegistry` résout le provider par `providerId` (`"fmp"`)

4. **And** test WireMock avec fixture `profile-inveb.json` valide le mapping

_FRs : FR-1 · NFRs : NFR-1, NFR-7_

## Tasks / Subtasks

- [x] Créer `PriceQuote` record et interface `PriceProvider` (AC: #1, #3)
  - [x] Package `features/assets/price/` — signatures ADR-04
- [x] Implémenter `FmpPriceProvider` (AC: #1, #2)
  - [x] Appeler `FMPClient.getData(asset.symbol())` — pas de symbole hardcodé
  - [x] Mapper `FMPDto[0]` → `PriceQuote` ; `currency` depuis `asset.currency()`
  - [x] `fetchedAt = Instant.now()` au moment du fetch réussi
  - [x] `providerId()` retourne `"fmp"` (lowercase, aligné YAML `provider: fmp`)
- [x] Implémenter `PriceProviderRegistry` (AC: #3)
  - [x] Injection Spring de toutes les implémentations `PriceProvider`
  - [x] `Optional<PriceProvider> findById(String providerId)` — lookup case-insensitive
  - [x] `IllegalArgumentException` claire si provider inconnu (ex. `"scrape"` avant Epic 5)
- [x] Ajouter dépendance WireMock test + fixture JSON (AC: #4)
  - [x] `backend/src/test/resources/fixtures/fmp/profile-inveb.json`
  - [x] `FmpPriceProviderTest` — WireMock stub `/profile`, pas de mock Mockito du client HTTP
- [x] Vérifier `mvn test` vert — ne pas casser `InveBService` ni `AssetRegistryTest`

## Dev Notes

### Story scope — IN / OUT

| IN scope | OUT of scope (stories suivantes) |
|----------|----------------------------------|
| `PriceProvider`, `PriceQuote`, `FmpPriceProvider`, `PriceProviderRegistry` | `ConfigurableAssetService` (1.3) |
| Test WireMock mapping FMP → `PriceQuote` | `ScrapePriceProvider` (5.2) |
| Bean Spring pour registry | Refactor `InveBService` pour utiliser le provider (1.3 / 4.5) |
| Fixture `profile-inveb.json` | `AssetDto`, endpoint générique (1.4, 1.5) |
| Dépendance WireMock dans `pom.xml` | `ProviderCallMetrics` (2.3) |

**Ne pas toucher** `InveBService` dans cette story — il continue d'appeler `FMPClient` directement jusqu'à la story 1.3.

### Architecture compliance (ADR-04)

Package cible (architecture §4) :

```text
backend/src/main/java/com/dokkcorp/dashboard/features/assets/price/
├── PriceProvider.java          # interface
├── PriceQuote.java             # record
├── FmpPriceProvider.java       # @Service
└── PriceProviderRegistry.java  # @Component
```

Signatures attendues :

```java
public interface PriceProvider {
    String providerId();  // "fmp" | "scrape" (scrape = Epic 5)
    PriceQuote fetch(AssetDefinition asset);
}

public record PriceQuote(
    double price,
    String currency,
    Double marketCap,           // nullable
    Double changePercent24h,    // nullable
    Double volume,              // nullable
    Instant fetchedAt
) {}
```

Résolution registry :

```java
// AssetProvider.FMP → providerId "fmp"
registry.findById("fmp").orElseThrow(...).fetch(assetDefinition);
```

Lier `AssetProvider` enum au `providerId` string :

```java
// Suggestion : méthode sur AssetProvider
public String providerId() {
    return name().toLowerCase(Locale.ROOT);  // FMP → "fmp", SCRAPE → "scrape"
}
```

### Mapping FMPDto → PriceQuote

`FMPClient` appelle déjà `GET /stable/profile?symbol={symbol}&apikey=...` via `ExternalCallExecutor` — **ne pas dupliquer** retry/timeout dans `FmpPriceProvider`.

| PriceQuote field | Source |
|------------------|--------|
| `price` | `FMPDto.currentPrice()` |
| `currency` | `AssetDefinition.currency()` — **pas** dans `FMPDto` actuel |
| `marketCap` | `FMPDto.marketCap()` (boxed `Double` pour nullabilité future scrape) |
| `changePercent24h` | `FMPDto.priceChangePercentage24h()` |
| `volume` | `FMPDto.totalVolume()` |
| `fetchedAt` | `Instant.now()` après réponse FMP valide |

Appel : `fmpClient.getData(asset.symbol())` — pour inveb → `"INVE-B.ST"` (depuis registre, pas hardcodé).

`FMPDto` actuel (`providers/fmp/FMPDto.java`) :

```java
public record FMPDto(
    String symbol,
    @JsonAlias("price") double currentPrice,
    @JsonAlias("marketCap") double marketCap,
    @JsonAlias("changePercentage") double priceChangePercentage24h,
    @JsonAlias("volume") double totalVolume
) {}
```

### ExternalCallExecutor (NFR-7)

`FMPClient.getData()` encapsule déjà `externalCallExecutor.execute(...)` avec retry Spring (`max-attempts: 3`, `backoff-ms: 300` depuis `application.yml`).

`FmpPriceProvider` doit **déléguer** à `FMPClient` — ne pas appeler `RestClient` directement. Les `ExternalProviderException` remontent telles quelles ; pas de catch silencieux ici (le fallback cache est responsabilité de `ConfigurableAssetService` en 1.3).

### FMP configuration existante

```yaml
# application.yml
app:
  fmp:
    api-key: "${FMP_API_KEY:}"
    base-url: "https://financialmodelingprep.com/stable"
```

Endpoint réel : `{base-url}/profile?symbol=INVE-B.ST&apikey=...`

### Test WireMock (ADR-15)

WireMock **n'est pas encore** dans `pom.xml` — l'ajouter en scope `test` :

```xml
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <version>3.13.1</version>
    <scope>test</scope>
</dependency>
```

Fixture `backend/src/test/resources/fixtures/fmp/profile-inveb.json` — format tableau FMP `/profile` :

```json
[
  {
    "symbol": "INVE-B",
    "price": 245.5,
    "marketCap": 1000000000,
    "changePercentage": -1.25,
    "volume": 123456,
    "currency": "SEK"
  }
]
```

Note : `currency` dans le JSON FMP est ignoré par `FMPDto` — le test doit assert `quote.currency()` == `"SEK"` depuis `AssetDefinition`, pas depuis le JSON.

Pattern test recommandé :

1. Démarrer WireMock sur port dynamique.
2. Instancier `FMPClient` avec `RestClient.Builder.baseUrl(wireMock.baseUrl())` + `ExternalCallExecutor` réel (ou `@SpringBootTest` avec `@DynamicPropertySource` pour override `app.fmp.base-url`).
3. Stub `GET /profile?symbol=INVE-B.ST` → fixture JSON.
4. `FmpPriceProvider.fetch(invebAssetDefinition)` → assert tous les champs `PriceQuote`.
5. Test négatif : stub 500 → assert `ExternalProviderException` propagée.

Alternative acceptable si WireMock + RestClient pose friction : `@SpringBootTest` + `@DynamicPropertySource` pointant `app.fmp.base-url` vers WireMock — **pas** de `@MockBean FMPClient` pour le test d'intégration mapping (AC exige WireMock).

Test unitaire séparé (Mockito) pour `PriceProviderRegistry` : enregistre un fake provider, vérifie lookup `"fmp"` / case-insensitive / provider inconnu.

### Previous story intelligence (1.1)

- `AssetDefinition` record : champs `id`, `displayName`, `provider` (`AssetProvider` enum), `symbol`, `dbSymbol`, `type`, `currency`, `marketHours`, `sync`, `scrapeParser`.
- `findById` normalise en lowercase — utiliser `"inveb"` dans les tests.
- `AssetProvider.fromYaml("fmp")` → `AssetProvider.FMP`.
- Validation au démarrage via `AssetRegistryMapper` — messages contextualisés par `assetId`.
- Registre prod : seule entrée `inveb` pour l'instant (`config/assets-registry.yml`).

### File structure requirements

**NEW files :**

```text
backend/src/main/java/com/dokkcorp/dashboard/features/assets/price/
  PriceProvider.java
  PriceQuote.java
  FmpPriceProvider.java
  PriceProviderRegistry.java

backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/
  FmpPriceProviderTest.java
  PriceProviderRegistryTest.java

backend/src/test/resources/fixtures/fmp/
  profile-inveb.json
```

**MODIFY :**

```text
backend/pom.xml   # wiremock-standalone test dependency
```

**DO NOT MODIFY :**

```text
backend/src/main/java/com/dokkcorp/dashboard/providers/fmp/FMPClient.java   # déjà conforme
backend/src/main/java/com/dokkcorp/dashboard/features/stocks/investorab/InveBService.java
```

### Testing requirements

- `mvn test` depuis `backend/` — tous les tests existants + nouveaux verts.
- Minimum 2 nouvelles classes de test :
  - `FmpPriceProviderTest` — WireMock + fixture (AC #4)
  - `PriceProviderRegistryTest` — lookup, case-insensitive, unknown provider
- Réutiliser les valeurs de `InveBServiceTest.stubFmp()` comme référence : prix `245.5`, marketCap `1_000_000_000`, change `-1.25`, volume `123_456`.
- Pas de test E2E, pas de modification front (story backend pure — @steve).

### Anti-patterns à éviter

- **Ne pas** créer un second client HTTP FMP — réutiliser `FMPClient`.
- **Ne pas** hardcoder `"INVE-B.ST"` dans `FmpPriceProvider` — utiliser `asset.symbol()`.
- **Ne pas** implémenter `ScrapePriceProvider` ni factory extensible (non-goal architecture §12).
- **Ne pas** ajouter logique cache/BD/market hours — c'est `ConfigurableAssetService` (1.3).
- **Ne pas** modifier `FMPDto` sauf si le mapping JSON réel l'exige — le champ `currency` vient du registre.

### Project Structure Notes

- Alignement architecture M0 (phase cadre) — première brique prix avant service générique.
- `AssetProvider.providerId()` (méthode suggérée) évite la duplication `"fmp"` string dans le code.
- Steve uniquement (`backend/**`) — pas de changement `frontend/**`.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-1.2]
- [Source: _bmad-output/planning-artifacts/architecture.md#ADR-04]
- [Source: _bmad-output/planning-artifacts/architecture.md#ADR-15]
- [Source: _bmad-output/planning-artifacts/architecture.md#4-Structure-packages-backend]
- [Source: backend/src/main/java/com/dokkcorp/dashboard/providers/fmp/FMPClient.java]
- [Source: backend/src/main/java/com/dokkcorp/dashboard/providers/fmp/FMPDto.java]
- [Source: backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetDefinition.java]
- [Source: _bmad-output/implementation-artifacts/stories/1-1-registre-actifs-yaml-et-assetregistry.md]

## Dev Agent Record

### Agent Model Used

claude (Cursor Agent)

### Debug Log References

### Completion Notes List

- `PriceProvider` / `PriceQuote` / `FmpPriceProvider` / `PriceProviderRegistry` implémentés dans `features/assets/price/`
- `FmpPriceProvider` délègue à `FMPClient` (retry via `ExternalCallExecutor` inchangé)
- `currency` mappé depuis `AssetDefinition`, pas depuis la réponse FMP
- `AssetProvider.providerId()` ajouté pour alignement enum ↔ registry
- Tests : `FmpPriceProviderTest` (WireMock + fixture), `PriceProviderRegistryTest` (lookup, case-insensitive, unknown)
- `mvn test` vert — aucune régression sur `InveBService` / `AssetRegistryTest`

### File List

- `backend/pom.xml`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetProvider.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/price/PriceProvider.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/price/PriceQuote.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProvider.java`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/price/PriceProviderRegistry.java`
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/FmpPriceProviderTest.java`
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/price/PriceProviderRegistryTest.java`
- `backend/src/test/resources/fixtures/fmp/profile-inveb.json`

## Change Log

- 2026-06-17 — Story context créée (bmad-create-story)
- 2026-06-18 — Implémentation Story 1.2 complète, statut `review`
- 2026-06-18 — Marquée `done` (validation utilisateur)
