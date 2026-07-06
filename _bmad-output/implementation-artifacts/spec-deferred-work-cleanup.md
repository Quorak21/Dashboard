---
title: 'Nettoyage deferred work post-épic 6'
type: 'chore'
created: '2026-07-06'
status: 'done'
baseline_commit: '86f6da2'
context:
  - '{project-root}/_bmad-output/project-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Après les specs Quick Dev (routage dynamique, navbar), il reste du travail différé : un fallback hardcodé `displayNamesByAssetId` dans `asset-page.ts`, des changements stories 6.5/6.6 non finalisés dans le working tree (dividend-card, fundamentals-card, `retailIndustryWeights`, YAML), et un doute sur l'encodage UTF-8 des fichiers YAML.

**Approach:** Valider et corriger l'encodage YAML si nécessaire, supprimer tout hardcoding d'identifiants d'actifs côté front, finaliser et tester le périmètre 6.5/6.6 déjà présent dans le working tree, puis marquer les items comme résolus dans `deferred-work.md`.

## Boundaries & Constraints

**Always:**
- TypeScript strict, Angular 21 standalone, signals/computed.
- DTOs backend = records ; répercuter tout changement de contrat dans `frontend/src/app/core/models/`.
- Tests Vitest et JUnit doivent passer avant clôture.
- Ne pas modifier `code_review.md` ni `journal.md`.

**Ask First:**
- Si un fichier YAML contient réellement des octets corrompus (pas un artefact console Windows), confirmer la correction avant réécriture massive.

**Never:**
- Ne pas réintroduire de map statique d'IDs d'actifs côté frontend.
- Ne pas activer Spring Security.
- Ne pas committer (l'utilisateur décide du commit).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Display name from API | `AssetDto.displayName = "Realty Income"` | `assetName()` retourne `"Realty Income"` | N/A |
| Missing display name | `AssetDto.displayName = null` | `assetName()` retourne `''` (pas de fallback hardcodé) | N/A |
| REIT retail breakdown | `fundamentals.retailIndustryWeights` peuplé (asset `o`) | `FundamentalsCard` affiche la section retail | Section masquée si liste vide |
| YAML UTF-8 | Fichiers `fundamentals/*.yml`, `dividends/*.yml` | Caractères `—`, `…`, `£` lisibles et valides | Corriger uniquement si corruption confirmée dans le fichier |

</frozen-after-approval>

## Code Map

- `frontend/src/app/shared/components/asset-page/asset-page.ts` -- Supprimer `displayNamesByAssetId` ; `assetName` = `data()?.displayName ?? ''`
- `frontend/src/app/shared/components/asset-page/asset-page.spec.ts` -- Retirer le test fallback hardcodé ; ajouter test `displayName` null → chaîne vide
- `frontend/src/app/shared/components/dividend-card/dividend-card.ts` -- Finaliser calcul CAGR côté composant (changements 6.5 déjà en cours)
- `frontend/src/app/shared/components/dividend-card/dividend-card.html` -- Template aligné sur la logique TS
- `frontend/src/app/shared/components/dividend-card/dividend-card.spec.ts` -- Couvrir CAGR et historique 10 ans
- `frontend/src/app/shared/components/fundamentals-card/fundamentals-card.ts` -- `retailIndustryWeights` computed + labels métriques étendus
- `frontend/src/app/shared/components/fundamentals-card/fundamentals-card.html` -- Section retail industry weights conditionnelle
- `frontend/src/app/shared/components/fundamentals-card/fundamentals-card.spec.ts` -- Tests retail weights et métriques REIT/ETF
- `frontend/src/app/core/models/asset.dto.ts` -- `retailIndustryWeights` sur `FundamentalsBlock`
- `backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/FundamentalsBlock.java` -- Champ `retailIndustryWeights`
- `backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsProperties.java` -- Binding YAML `retail-industry-weights`
- `backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsConfiguration.java` -- Mapping vers DTO
- `backend/src/main/resources/config/fundamentals/o.yml` -- Données retail industry (REIT)
- `backend/src/main/resources/config/fundamentals/brwm.yml` -- Métriques ETF mining
- `backend/src/main/resources/config/fundamentals/inveb.yml` -- Métriques holding
- `backend/src/main/resources/config/dividends/*.yml` -- Dividendes brwm, inveb, o
- `backend/src/test/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsPropertiesTest.java` -- Validation binding retail weights
- `backend/src/test/java/com/dokkcorp/dashboard/features/assets/ConfigurableAssetServiceTest.java` -- Service expose retail weights
- `_bmad-output/implementation-artifacts/deferred-work.md` -- Retirer les items résolus après implémentation

## Tasks & Acceptance

**Execution:**
- [x] `backend/src/main/resources/config/fundamentals/brwm.yml` -- Vérifier encodage UTF-8 (`—`, `…`) ; corriger si octets invalides -- Éliminer fausse alerte ou corruption réelle
- [x] `backend/src/main/resources/config/fundamentals/inveb.yml` -- Idem validation encodage
- [x] `backend/src/main/resources/config/fundamentals/o.yml` -- Idem validation encodage
- [x] `backend/src/main/resources/config/dividends/brwm.yml` -- Idem validation encodage
- [x] `backend/src/main/resources/config/dividends/inveb.yml` -- Idem validation encodage
- [x] `backend/src/main/resources/config/dividends/o.yml` -- Idem validation encodage
- [x] `frontend/src/app/shared/components/asset-page/asset-page.ts` -- Supprimer `displayNamesByAssetId` ; simplifier `assetName` computed
- [x] `frontend/src/app/shared/components/asset-page/asset-page.spec.ts` -- Remplacer test fallback par test `displayName` null → `''`
- [x] `backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsProperties.java` -- Finaliser binding `retailIndustryWeights` (déjà partiellement modifié)
- [x] `backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsConfiguration.java` -- Mapper `retailIndustryWeights` vers `FundamentalsBlock`
- [x] `backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/FundamentalsBlock.java` -- Confirmer champ `retailIndustryWeights` dans le record
- [x] `frontend/src/app/core/models/asset.dto.ts` -- Confirmer `retailIndustryWeights?: SectorWeight[]` sur `FundamentalsBlock`
- [x] `frontend/src/app/shared/components/fundamentals-card/fundamentals-card.ts` -- Finaliser computed `retailIndustryWeights` et affichage conditionnel
- [x] `frontend/src/app/shared/components/fundamentals-card/fundamentals-card.html` -- Section retail industry si données présentes
- [x] `frontend/src/app/shared/components/fundamentals-card/fundamentals-card.spec.ts` -- Tests retail weights passent
- [x] `frontend/src/app/shared/components/dividend-card/dividend-card.ts` -- Finaliser CAGR calculé depuis historique
- [x] `frontend/src/app/shared/components/dividend-card/dividend-card.spec.ts` -- Tests CAGR et historique passent
- [x] `_bmad-output/implementation-artifacts/deferred-work.md` -- Supprimer les 3 sections résolues ou marquer comme done

**Acceptance Criteria:**
- Given une page asset avec `displayName` fourni par l'API, when la page charge, then le titre affiché provient uniquement de `AssetDto.displayName`.
- Given une page asset avec `displayName: null`, when la page charge, then `assetName()` est une chaîne vide et aucune map statique n'est consultée.
- Given l'asset `o` avec `retail-industry-weights` dans le YAML, when l'API retourne les fundamentals, then la carte fundamentals affiche la ventilation retail.
- Given les fichiers YAML fundamentals/dividends, when lus en UTF-8, then les caractères spéciaux (`—`, `…`) sont corrects ou corrigés.
- Given le working tree actuel, when `mvn test` et `ng test` s'exécutent, then tous les tests passent.

## Spec Change Log

## Verification

**Commands:**
- `cd backend && mvn test` -- expected: BUILD SUCCESS, 0 failures
- `cd frontend && ng test --no-watch` -- expected: all specs pass

**Manual checks (if no CLI):**
- Ouvrir `/asset/o` : section retail industry visible si données backend présentes.
- Vérifier qu'aucune occurrence de `displayNamesByAssetId` ne subsiste dans le repo.

## Suggested Review Order

**Suppression hardcoding & contrat API**

- `displayName` provient uniquement de l'API, plus de fallback statique
  [`asset-page.spec.ts:171`](../../../frontend/src/app/shared/components/asset-page/asset-page.spec.ts#L171)

- Factory d'erreur enrichie avec `displayName` et `type` du registre
  [`AssetDto.java:25`](../../../backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/AssetDto.java#L25)

**Retail industry weights (REIT)**

- Nouveau champ YAML `retail-industry-weights` bindé côté properties
  [`AssetFundamentalsProperties.java:29`](../../../backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsProperties.java#L29)

- Validation partagée sector + retail weights
  [`AssetFundamentalsConfiguration.java:127`](../../../backend/src/main/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsConfiguration.java#L127)

- Record DTO backend exposant la ventilation retail
  [`FundamentalsBlock.java:14`](../../../backend/src/main/java/com/dokkcorp/dashboard/features/assets/model/FundamentalsBlock.java#L14)

- Section retail conditionnelle dans la carte fundamentals
  [`fundamentals-card.html:74`](../../../frontend/src/app/shared/components/fundamentals-card/fundamentals-card.html#L74)

**Dividendes & données YAML**

- CAGR calculé depuis l'historique dividendes côté composant
  [`dividend-card.ts:7`](../../../frontend/src/app/shared/components/dividend-card/dividend-card.ts#L7)

- Données retail industry pour l'asset `o`
  [`o.yml:15`](../../../backend/src/main/resources/config/fundamentals/o.yml#L15)

**Tests**

- Couverture retail weights fundamentals card
  [`fundamentals-card.spec.ts:100`](../../../frontend/src/app/shared/components/fundamentals-card/fundamentals-card.spec.ts#L100)

- Tests binding retail weights backend
  [`AssetFundamentalsPropertiesTest.java`](../../../backend/src/test/java/com/dokkcorp/dashboard/config/assets/AssetFundamentalsPropertiesTest.java)

