---
project_name: 'Dashboard'
user_name: 'Dokk'
date: '2026-06-17'
sections_completed:
  - technology_stack
  - language_rules
  - framework_rules
  - testing_rules
  - quality_rules
  - workflow_rules
  - anti_patterns
status: complete
rule_count: 42
optimized_for_llm: true
---

# Contexte Projet pour Agents IA

_Ce fichier contient les règles et patterns critiques que les agents IA doivent suivre lors de l'implémentation de code dans ce projet. Il se concentre sur les détails non évidents qu'un LLM pourrait oublier._

---

## Stack Technologique & Versions

| Couche | Technologie | Version |
|--------|-------------|---------|
| Frontend | Angular (standalone) | 21.2.x |
| Frontend | TypeScript | 5.9.x (strict) |
| Frontend | Tailwind CSS | 3.4.x |
| Frontend | Chart.js | 4.5.x |
| Frontend | Vitest | 4.0.x |
| Frontend | RxJS | 7.8.x |
| Backend | Spring Boot | 4.0.6 |
| Backend | Java | 21 |
| Backend | PostgreSQL | 16 |
| Backend | web3j | 4.12.2 |
| Infra | Docker Compose | prod VPS |
| Déploiement front | Vercel | `dashboard.dokkcorp.ch` |

**Actifs suivis :** HYPE (crypto/Hyperliquid) et InveB (action Investor AB).

---

## Règles d'Implémentation Critiques

### Règles Langage (TypeScript / Java)

**TypeScript (frontend)**
- `strict: true` activé — pas de `any` sauf tests (mock global `ResizeObserver`).
- Imports de types : `import type { X }` pour les DTOs.
- Injection : `inject()` préféré à l'injection constructeur.
- Nettoyage RxJS : `takeUntilDestroyed(this.destroyRef)` obligatoire sur les subscriptions.
- DTOs front miroir du back : `frontend/src/app/core/models/` (`HypeDto`, `InveBDto`).

**Java (backend)**
- DTOs = **records immutables** (`public record HypeDto(...)`).
- Sous-DTOs dans `features/{domaine}/{asset}/models/`.
- Pas de clés API en dur : placeholders `${VAR:default}` dans `application.yml`.
- `BigDecimal` pour les calculs financiers/crypto (pas de `double` pour la précision).

### Règles Framework

**Angular 21 (frontend)**
- **Standalone uniquement** — pas de `NgModule`.
- **Signals** : `signal()`, `computed()`, `input()` — pas de `BehaviorSubject` pour l'état local.
- **Control flow** : `@if`, `@for` avec `track` — pas de `*ngIf`/`*ngFor`.
- Routes lazy : `loadComponent: () => import(...).then(m => m.X)`.
- Structure :
  - `core/` — services, models, composants layout (navbar, footer)
  - `features/{crypto|stocks}/{asset}/` — pages et composants métier
  - `shared/` — composants réutilisables (charts, cards, pipes)
- API : `DashboardApiService` → `GET /api/dashboard/{path}`.
- Erreurs HTTP : toast via `ToastService`, retour `of(null)` — pas de throw côté composant.

**Spring Boot 4 (backend)**
- Architecture par feature : `features/crypto/hype/`, `features/stocks/investorab/`.
- Providers externes isolés : `providers/coingecko/`, `providers/hyperliquid/`, `providers/fmp/`, `providers/blockchain/`.
- Controller unique : `DashboardController` sous `/api/dashboard`.
- CORS : `${app.cors.allowed-origins}` — ne pas hardcoder les origines.
- Appels externes : `RestClient` + `ExternalCallExecutor` (retry, timeouts configurables).
- JPA : entités `AssetSnapshot`, `AssetDaily` + repositories dédiés.
- Job planifié : `AssetSyncJob` pour la synchro des données.
- Sécurité Spring **désactivée** (commentée) — pas d'auth pour l'instant.
- Exceptions : `GlobalExceptionHandler` → message générique, jamais de stack trace au client.

### Règles de Tests

**Frontend (Vitest)**
- Fichiers `*.spec.ts` colocalisés avec le composant.
- Mock API : spy `DashboardApiService.getData` avec `vi.fn().mockReturnValue(of(...))`.
- Mock `ResizeObserver` dans `beforeAll` pour les tests de charts.
- Commande : `ng test` (Vitest via Angular CLI).

**Backend (JUnit 5)**
- Tests colocalisés : `src/test/java/...` miroir du package source.
- Cibler services, mappers, calculateurs (`HypeCalculator`, `HypeMapper`).
- Commande : `mvn test` depuis `backend/`.

### Qualité & Style

**Formatage**
- Prettier sur le frontend (`npx prettier --write <fichier>`).
- Pas de ESLint configuré — respecter les conventions existantes.

**Nommage**
- Fichiers composants Angular : kebab-case (`hype-metric-card.ts`).
- Classes exportées : PascalCase sans suffixe `Component` (`Hype`, pas `HypeComponent`).
- Sélecteurs : `app-{kebab-case}`.
- Packages Java : `com.dokkcorp.dashboard.{layer}`.

**UI / Tailwind**
- Palette : `dark-900/800/700` (fonds), `copper-300` à `copper-700` (accents).
- Config : `frontend/tailwind.config.js` — ne pas inventer de couleurs hors palette.
- Responsive mobile-first obligatoire.
- Budgets prod : initial ≤ 500 kB warning / 1 MB error.

**Dette technique**
- Voir `backlog.md` (actif) et `journal.md` (résolu).
- Règles agent : `.agents/AGENTS.md`.

### Workflow de Développement

**Git**
- Pas de commit sauf demande explicite de l'utilisateur.
- Pas de secrets en clair (`.env`, clés API).

**Environnements**
- Dev front : `localhost:4200` → API `localhost:8080`.
- Dev back : profil `dev`, Postgres local `dokkcorp_db`.
- Prod : Docker Compose (Postgres 1G, backend Java 1.5G RAM max).
- Variables prod : `COINGECKO_API_KEY`, `BLOCKCHAIN_RPC_URL`, `FMP_API_KEY`, `CORS_ORIGINS`.

**Contrat API**
- Tout changement de DTO backend doit être répercuté dans `frontend/src/app/core/models/`.

### Règles Critiques « Ne Pas Oublier »

- **RAM VPS** : limiter caches et `-Xmx` Java — pas de structures en mémoire inutiles.
- **Pas de Spring Security** activé — ne pas ajouter d'auth sans décision explicite.
- **Providers externes** : toujours passer par les clients existants, pas d'appel HTTP direct dans les services métier.
- **Charts** : Chart.js côté front — données pré-formatées par le back, pas de recalcul métier côté UI.
- **Erreurs utilisateur** : messages en français côté front (toasts).
- **404 front** : redirige vers `/` (dashboard), pas de page 404 dédiée.
- **HypeCalculator** : logique métier crypto centralisée dans `maths/` — ne pas dupliquer les formules.
- **ddl-auto: update** en dev — attention aux migrations prod (pas de Flyway configuré actuellement).

---

## Guide d'Utilisation

**Pour les agents IA :**
- Lire `.agents/AGENTS.md` avant toute implémentation.
- Ce fichier (`docs/project-context.md`) complète AGENTS.md pour les détails non évidents.

**Pour les humains :**
- Garder ce fichier concis et orienté agents.
- Mettre à jour lors des changements de stack.
- Revoir trimestriellement pour retirer les règles devenues évidentes.

_Dernière mise à jour : 2026-06-17_
