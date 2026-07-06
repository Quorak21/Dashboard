# Règles du Projet (Dashboard)

Ces règles s'appliquent à l'agent unique travaillant sur cette base de code.

## Cap produit

**Dashboard personnel** de suivi d'actifs financiers (crypto + actions + ETF) :
- Vue **overview** avec cartes par actif (prix live, variation, métadonnées registry).
- Pages **détail** par actif : graphiques, dividendes, fondamentaux, alertes trimestrielles.
- **Registry YAML** (`assets-registry.yml`) : ajouter un actif = config, pas un nouveau service par symbole.
- Déploiement : front **Vercel** (`dashboard.dokkcorp.ch`), back **Docker** sur VPS (`api.dokkcorp.ch`).

**Actifs suivis** : HYPE (crypto), InveB, BRWM, Realty Income (O), 3i Group (III), iShares Global Infrastructure (INFR), etc.

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Frontend | Angular 21 (standalone, Signals), TypeScript strict, Tailwind 3.4, Chart.js, Vitest |
| Backend | Spring Boot 4, Java 21, PostgreSQL 16, RestClient, JPA |
| Infra | Docker Compose prod, Watchtower, GitHub Actions (`mvn test` + push image) |

**Tests** : `ng test` dans `frontend/` · `mvn test` dans `backend/`.

## Simplicité (priorité absolue)

- **Pas d'over-engineering** : solution minimale qui répond au besoin. Un fichier, une responsabilité.
- **Pas de nouvelle abstraction** sans demande explicite (registry, providers, etc. existent déjà — ne pas en empiler).
- **Comprendre avant d'étendre** : l'utilisateur est en apprentissage — expliquer les changements non triviaux en langage simple.
- **Diff minimal** : ne pas toucher au code hors périmètre de la demande.

## Suivi projet

| Fichier | Rôle |
|---------|------|
| `backlog.md` | Tâches en cours (DEF / dettes / bugs) + **FF** (features futures) |
| `journal.md` | Jalons et tâches **terminées** (une ligne par ticket résolu) |

- Lire `backlog.md` avant une grosse session.
- Ticket résolu → retirer du backlog + une ligne dans `journal.md`.
- Ne jamais recycler un ID de ticket existant.
- Jamais de secrets en clair dans le repo.

## Frontend (Angular 21)

**Architecture**
- `core/` — services, models, layout (navbar, footer)
- `features/{crypto|stocks}/{asset}/` — pages métier
- `shared/` — composants réutilisables (charts, cards, pipes)
- Standalone uniquement — pas de `NgModule`.
- Signals : `signal()`, `computed()`, `input()` — pas de `BehaviorSubject` pour l'état local.
- Control flow : `@if`, `@for` avec `track`.
- Routes lazy : `loadComponent: () => import(...)`.
- Injection : `inject()` préféré au constructeur.
- RxJS : `takeUntilDestroyed(this.destroyRef)` sur les subscriptions.

**TypeScript**
- `strict: true` — pas de `any` sauf tests.
- `import type { X }` pour les DTOs.
- DTOs miroir du back dans `frontend/src/app/core/models/`.

**UI / Tailwind**
- Thème **dark & copper** : `dark-900/800/700`, accents `copper-300` → `copper-700`.
- Config : `frontend/tailwind.config.js` — pas de couleurs hors palette.
- Responsive mobile-first.
- API : `DashboardApiService` → `GET /api/dashboard/...`.
- Erreurs HTTP : toast via `ToastService`, retour `of(null)` — pas de throw côté composant.
- Messages utilisateur en **français**.

**Validation front**
1. `npx prettier --write` sur les fichiers modifiés.
2. Compilation TypeScript OK.
3. `ng test` — tous les tests passent.

## Backend (Spring Boot 4)

**Architecture**
- Par feature : `features/crypto/hype/`, `features/assets/`, etc.
- Providers externes isolés : `providers/coingecko/`, `hyperliquid/`, `fmp/`, `blockchain/`.
- Controller unique : `DashboardController` sous `/api/dashboard`.
- Registry : `ConfigurableAssetService` + `assets-registry.yml`.

**Java**
- DTOs = **records** immutables.
- `BigDecimal` pour calculs financiers — pas de `double` pour la précision.
- Pas de clés API en dur : `${VAR:default}` dans `application.yml`.
- Appels externes : `RestClient` + `ExternalCallExecutor` (retry, timeouts).
- Exceptions : `GlobalExceptionHandler` — message générique au client, jamais de stack trace.

**Contraintes VPS**
- Limiter RAM : caches bornés, `-XX:MaxRAMPercentage=75.0` dans le Dockerfile.
- Pas de Spring Security activé — pas d'auth sans décision explicite.

**Validation back**
1. `mvn test` depuis `backend/`.
2. Signaler tout changement de contrat API (DTO) → adapter `frontend/src/app/core/models/`.

## Règles transverses

- **Contrat API** : tout changement DTO back = mise à jour des models front.
- **Providers** : passer par les clients existants, pas d'HTTP direct dans les services métier.
- **Charts** : données pré-formatées par le back — pas de recalcul métier côté UI.
- **HypeCalculator** : logique crypto centralisée dans `maths/` — ne pas dupliquer.
- **Git** : pas de commit sauf demande explicite de l'utilisateur.
- **Prod** : `ddl-auto: validate` — attention aux migrations de schéma.
- **404 front** : redirect vers `/`, pas de page 404 dédiée.

## Documentation détaillée

Référence complémentaire (patterns, anti-patterns) : `docs/project-context.md`.
