---
title: 'P1 — Bounded cache maps in ConfigurableAssetService'
type: chore
created: '2026-06-18'
status: done
baseline_commit: '35ff7e019c6faf63eee0585a9bda731d54513a27'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-retro-2026-06-18.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `ConfigurableAssetService` keeps unbounded `ConcurrentHashMap` entries for `cacheByAssetId`, `syncLocks`, and `historyStateByAssetId`. Orphaned keys survive after an asset leaves the registry, which becomes a memory leak once `AssetSyncJob` (Story 2.2) syncs every registry asset on a schedule.

**Approach:** Align all three maps with `AssetRegistry.all()` at startup and expose a reusable sync method for future registry reload. Evict keys not present in the current registry; pre-initialize slots only for registered asset ids.

## Boundaries & Constraints

**Always:** Maps must never retain keys absent from the current registry after sync. Use `assetRegistry.all()` as the single source of truth. Keep existing public API (`getData`, `syncPrice`) unchanged.

**Ask First:** Changing `AssetRegistry` to support hot reload at runtime.

**Never:** Introduce a global cache eviction TTL unrelated to registry membership. Do not modify sprint-status story keys. Do not touch frontend.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Startup align | Registry lists `inveb` only | Maps contain exactly `inveb` keys (empty cache slots) | N/A |
| Orphan eviction | Maps hold `inveb` + `removed`; registry lists `inveb` only | After sync, maps contain only `inveb` | N/A |
| Valid sync after align | Registered asset `inveb` | `syncPrice` / `getData` behave as before | Existing error paths unchanged |
| Unknown asset id | `assetId` not in registry | `AssetNotFoundException` before any map growth | Unchanged |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java` — three unbounded maps; add registry-aligned sync + `@PostConstruct`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/AssetRegistry.java` — `all()` is registry source of truth
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetServiceTest.java` — add eviction + startup alignment tests

## Tasks & Acceptance

**Execution:**
- [x] `ConfigurableAssetService.java` — add `synchronizeCachesWithRegistry()` and `@PostConstruct initializeCachesFromRegistry()` — bound maps to registry ids
- [x] `ConfigurableAssetServiceTest.java` — test startup alignment and orphan eviction after registry shrink

**Acceptance Criteria:**
- Given the service starts with a registry of N assets, when initialization runs, then each internal map contains exactly those N asset ids and no others
- Given maps contain entries for assets no longer in the registry, when `synchronizeCachesWithRegistry()` runs, then orphaned keys are removed from all three maps
- Given existing unit tests, when `mvn test` runs, then all tests pass

## Spec Change Log

## Verification

**Commands:**
- `.\mvnw.cmd test -Dtest=ConfigurableAssetServiceTest` — BUILD SUCCESS (9 tests)
- Full suite not re-run in this session

## Suggested Review Order

- Registry-aligned cache sync at startup and on demand
  [`ConfigurableAssetService.java:62`](../../backend/src/main/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetService.java#L62)

- Orphan eviction and startup alignment tests
  [`ConfigurableAssetServiceTest.java:222`](../../backend/src/test/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetServiceTest.java#L222)
