---
story_id: 4.6
story_key: 4-6-carte-overview-inveb-et-bandeau-alertes
epic: 4
epic_name: UI générique et migration Investor AB
status: done
baseline_commit: 35ff7e0
created: 2026-06-30
FRs:
  - FR-15
dependencies:
  - 4.5 (Façade InveBService et YAML inveb)
---

# Story 4.6: Carte overview InveB et bandeau alertes

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

En tant que Dokk,
je veux la carte overview InveB via le nouveau modèle et un bandeau d'alertes fondamentaux,
afin de voir la santé du portefeuille dès l'accueil.

## Acceptance Criteria

### AC-1 : Consommation de `getAsset('inveb')` pour la carte InveB
- Sur le dashboard (`/`), la carte d'overview pour Investor AB consomme le flux de données générique `api.getAsset('inveb')` (retournant un `AssetDto`) au lieu du service legacy `api.getData('inveb')`.
- Le prix (`invebPrice`) et la variation (`invebChange`) de la carte d'accueil sont correctement calculés depuis l'objet `AssetDto` obtenu.

### AC-2 : Bandeau `QuarterlyAlertsBanner` sous la Navbar
- Un composant `QuarterlyAlertsBanner` autonome est créé dans `frontend/src/app/shared/components/quarterly-alerts-banner/`.
- Le composant appelle `api.getQuarterlyAlerts()` au chargement pour récupérer les alertes sur les fondamentaux obsolètes (`QuarterlyAlertsResponse`).
- S'il y a des alertes, le bandeau s'affiche sous la navbar de manière responsive et moderne.
- Le texte affiché est en français : **« Fondamentaux à vérifier : [Liste des actifs] »** (par exemple : *« Fondamentaux à vérifier : Investor AB »*).
- Chaque actif listé dans le bandeau est cliquable et redirige l'utilisateur vers sa page de détail (par exemple: `/inveb`).
- Si la liste d'alertes est vide ou si l'API renvoie une erreur, le bandeau n'est pas affiché (affichage conditionnel).

### AC-3 : Couverture de tests
- Les tests unitaires du dashboard (`dashboard.spec.ts`) sont mis à jour pour mocker l'appel à `getAsset('inveb')` et vérifier le bon affichage des valeurs.
- Des tests unitaires sont ajoutés pour `QuarterlyAlertsBanner` dans `quarterly-alerts-banner.spec.ts` pour valider l'affichage du bandeau lorsqu'une alerte est présente, et son masquage quand aucune alerte n'est présente.
- Tous les tests de l'application front (`npm run test` ou `vitest`) passent avec succès.

## Tasks / Subtasks

- [x] Implémenter le composant `QuarterlyAlertsBanner` (AC-2)
  - [x] Créer les fichiers [quarterly-alerts-banner.ts](file:///frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.ts), [quarterly-alerts-banner.html](file:///frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.html) et [quarterly-alerts-banner.css](file:///frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.css)
  - [x] Implémenter la logique TypeScript avec Angular 21 (standalone component, injection de `DashboardApiService` et `DestroyRef`, gestion du cycle de vie RxJS avec `takeUntilDestroyed`)
  - [x] Styliser le bandeau en utilisant Tailwind CSS (accent cuivré/copper, fond sombre, animations de transition discrètes)
  - [x] Ajouter les liens de redirection vers les pages des actifs (ex: `[routerLink]="['/' + alert.assetId]"`)
- [x] Intégrer le bandeau dans le layout principal (AC-2)
  - [x] Importer `QuarterlyAlertsBanner` dans [app.ts](file:///frontend/src/app/app.ts)
  - [x] Ajouter le sélecteur `<app-quarterly-alerts-banner></app-quarterly-alerts-banner>` dans [app.html](file:///frontend/src/app/app.html) immédiatement sous `<app-navbar>`
- [x] Modifier la page Dashboard pour consommer l'API générique pour InveB (AC-1)
  - [x] Dans [dashboard.ts](file:///frontend/src/app/features/dashboard/dashboard.ts), remplacer l'appel à `api.getData('inveb')` par `api.getAsset('inveb')`
  - [x] Adapter les types et signaux pour `invebAsset = signal<AssetDto | null>(null)` et mettre à jour les computed properties
  - [x] Adapter [dashboard.html](file:///frontend/src/app/features/dashboard/dashboard.html) si nécessaire
- [x] Mettre à jour et ajouter les tests unitaires (AC-3)
  - [x] Adapter [dashboard.spec.ts](file:///frontend/src/app/features/dashboard/dashboard.spec.ts)
  - [x] Créer [quarterly-alerts-banner.spec.ts](file:///frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.spec.ts) et y ajouter des cas de test complets
  - [x] Lancer les tests front et vérifier le bon fonctionnement global

### Review Findings

- [x] [Review][Patch] Subscription Leak in Component Method [frontend/src/app/features/dashboard/dashboard.ts:38-47]
- [x] [Review][Patch] Uncaught Error in getAsset Subscription [frontend/src/app/features/dashboard/dashboard.ts:44-46]
- [x] [Review][Patch] Frontend Unit Test Failure (Timeout) [frontend/src/app/features/stocks/inveb/inveb.spec.ts:1]
- [x] [Review][Patch] Undefined Tailwind Copper Color Classes [frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.html:1]
- [x] [Review][Patch] Missing aria-hidden="true" on Decorative Icon [frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.html:3]
- [x] [Review][Patch] Potential Non-Array type on response.alerts [frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.ts:32-34]
- [x] [Review][Patch] Missing Guard for Null/Empty alert.assetId [frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.html:7]
- [x] [Review][Patch] Brittle String Concatenation in Router Link [frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.html:7]
- [x] [Review][Defer] Data Fetching in Constructor [frontend/src/app/features/dashboard/dashboard.ts:32-36] — deferred, pre-existing

## Dev Notes

### Architecture Patterns et Contraintes
- **Standalone Angular 21** : Tous les nouveaux composants doivent être configurés en Standalone (`standalone: true` et liste explicite d'imports).
- **Clean RxJS** : Toujours utiliser `.pipe(takeUntilDestroyed(this.destroyRef))` pour éviter les fuites de mémoire sur les abonnements asynchrones.
- **Tailwind Palette** : Utiliser la palette existante (fond sombre type `bg-dark-800` ou `bg-dark-900`, accents `text-copper-400`, bordures `border-copper-500/20`).

### Source tree components to touch
- [app.ts](file:///frontend/src/app/app.ts) (UPDATE)
- [app.html](file:///frontend/src/app/app.html) (UPDATE)
- [dashboard.ts](file:///frontend/src/app/features/dashboard/dashboard.ts) (UPDATE)
- [dashboard.html](file:///frontend/src/app/features/dashboard/dashboard.html) (UPDATE)
- [dashboard.spec.ts](file:///frontend/src/app/features/dashboard/dashboard.spec.ts) (UPDATE)
- [quarterly-alerts-banner/](file:///frontend/src/app/shared/components/quarterly-alerts-banner/) (NEW)

### References
- Fichier des Epics : [epics.md](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/planning-artifacts/epics.md#Story%204.6)
- Guide des règles du projet : [project-context.md](file:///c:/Users/chju/Desktop/Dashboard/_bmad-output/project-context.md)
- DTO des alertes front : [asset.dto.ts](file:///frontend/src/app/core/models/asset.dto.ts#L61)
- Service d'API du dashboard : [dashboard-api.service.ts](file:///frontend/src/app/core/services/dashboard-api.service.ts)

## Dev Agent Record

### Agent Model Used
Gemini 3.5 Flash (Medium)

### Debug Log References
- Exécution réussie de la suite de tests unitaires frontend (41 tests réussis sur 41).

### Completion Notes List
- Création du composant QuarterlyAlertsBanner dans `shared/components/quarterly-alerts-banner`.
- Intégration du bandeau d'alerte dans le layout principal `app.html` juste sous la barre de navigation.
- Migration de l'overview de la carte InveB vers l'API générique `api.getAsset('inveb')` à la place de l'ancienne API spécifique `api.getData('inveb')`.
- Ajout de tests unitaires complets pour valider le fonctionnement et l'affichage conditionnel du bandeau d'alerte en français.

### File List
- `frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.ts`
- `frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.html`
- `frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.css`
- `frontend/src/app/shared/components/quarterly-alerts-banner/quarterly-alerts-banner.spec.ts`
- `frontend/src/app/app.ts`
- `frontend/src/app/app.html`
- `frontend/src/app/features/dashboard/dashboard.ts`
- `frontend/src/app/features/dashboard/dashboard.spec.ts`
- `frontend/src/app/shared/components/asset-page/asset-page.spec.ts`
