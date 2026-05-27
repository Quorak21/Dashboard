# 📋 Dashboard — Backlog dette technique

> **Mise à jour** : 2026-05-26 · **Tâches actives** : 36
>
> Tâches résolues → `journal.md`. Jamais de secrets en clair.

---

## 🔴 CRITIQUE
<!-- Non urgent vu la taille du projet -->
- [ ] **SEC-05** — Aucune authentification sur l'API
  - 📁 `pom.xml` L43-48
  - `spring-boot-starter-security` est commenté. Tout est ouvert.
  - → À terme, ajouter l'auth sur les endpoints sensibles.

---

## 🟠 ÉLEVÉ

### Backend — Architecture

- [ ] **BACK-02** — Pas de `@ControllerAdvice` / gestion d'erreur globale
  - Aucun handler global. Les erreurs remontent comme HTTP 500 avec stack trace.
  - → Créer un `GlobalExceptionHandler` avec `@ControllerAdvice`

- [ ] **BACK-03** — `HypeService.mapToDto()` = 225 lignes
  - 📁 `HypeService.java` L157-382
  - Calculs financiers + requêtes DB + mapping DTO dans une seule méthode. Impossible à tester unitairement.
  - → Extraire en classes : `HypeCalculator`, `HypeMapper`, `FluxCalculator`

- [ ] **BACK-04** — `HypeService.getData()` trop long (~70 lignes)
  - 📁 `HypeService.java` L63-134
  - Mélange appels API, logique métier, persistance DB, et gestion du cache.
  - → Séparer la récupération de données, la logique métier, et la persistance.

- [ ] **BACK-05** — Pas de retry/timeout sur les appels API externes
  - 📁 `CoinGeckoClient.java`, `HyperliquidClient.java`, `FMPClient.java`, `BlockChainClient.java`
  - Aucun timeout configuré sur RestClient/Web3j. Si une API est lente, le thread est bloqué indéfiniment.
  - → Configurer `.connectTimeout()` et `.readTimeout()` sur RestClient, ajouter `spring-retry` ou `resilience4j`

- [ ] **BACK-06** — Appels API séquentiels dans HyperliquidClient (6 appels HTTP)
  - 📁 `HyperliquidClient.java` L24-45, `BlockChainClient.java` L44-52
  - → Paralléliser avec `CompletableFuture` ou Virtual Threads (Java 21)

- [ ] **BACK-07** — Clients API retournent `null` au lieu d'exceptions
  - 📁 `CoinGeckoClient.java` L38-41/L55-58, `FMPClient.java` L46
  - `getData()` et `getHistory()` retournent `null` en erreur. NPE potentiel en cascade.
  - → Lancer des exceptions métier typées, ou retourner `Optional`

### Backend — Données

- [ ] **BACK-08** — Types `String` pour des valeurs numériques partout
  - 📁 `AssetDaily.java` L25-30, `AssetSnapshot.java` L27-28, `HypeDto.java`, `HyperliquidDto.java`
  - Force des `Double.parseDouble()` partout (~25 appels), risque de `NumberFormatException`.
  - → Migrer vers `BigDecimal` pour financier, `double` pour métriques. Nécessite migration DB.

- [ ] **BACK-09** — `Long` pour les timestamps au lieu de types temporels
  - 📁 `AssetDaily.java` L24, `AssetSnapshot.java` L22
  - `System.currentTimeMillis()` stocké en Long. Illisible en DB, requêtes temporelles compliquées.
  - → Utiliser `Instant` avec `@Column(columnDefinition = "TIMESTAMP")`

- [ ] **BACK-10** — Pas d'index DB explicites
  - 📁 `AssetDaily.java`, `AssetSnapshot.java`
  - Les requêtes filtrent par `symbol` et trient par `lastRefresh`/`day`, mais aucun `@Index`.
  - → `@Table(indexes = @Index(columnList = "symbol, lastRefresh DESC"))`

### Infra / CI

- [ ] **INFRA-02** — Pas de tests dans le pipeline CI/CD
  - 📁 `Dockerfile` L6 (`-DskipTests`), `deploy.yml`
  - Un code qui casse les tests est déployé en prod automatiquement via Watchtower.
  - → Ajouter une étape `mvn test` avant le build, ou retirer `-DskipTests`

### Frontend — Typage

- [ ] **FRONT-01** — `Observable<any>` + `String` wrapper dans le service API
  - 📁 `dashboard-api.service.ts` L11
  - Aucun typage des réponses API. `String` wrapper au lieu de `string` primitif.
  - → Créer les interfaces `HypeDto`, `InveBDto` dans `core/models/`, typer les réponses.

- [ ] **FRONT-02** — `signal<any>(null)` dans les composants principaux
  - 📁 `hype.ts` L32, `inveb.ts` L18
  - 50+ `computed()` en cascade sans aucune sécurité de type.
  - → Typer avec l'interface DTO correspondante.

---

## 🟡 MOYEN

### Backend

- [ ] **BACK-11** — `Double` pour les valeurs financières dans les entités
  - 📁 `AssetDaily.java` L14-23, `AssetSnapshot.java` L14-26
  - Flottants IEEE754 causent des erreurs d'arrondi. OK pour lecture seule, problème si calculs.
  - → Migrer vers `BigDecimal` quand BACK-08 est fait.

- [ ] **BACK-13** — NPE potentiels dans HyperliquidClient
  - 📁 `HyperliquidClient.java` L132, L168-182
  - Chaîne d'accès `node.get(n).get("name").get("stats")...` sans null-check.
  - → Null-checks ou Optional sur les nœuds JSON.

- [ ] **BACK-14** — `HypeDto.error()` ignore le paramètre `symbol`
  - 📁 `HypeDto.java` L95-103
  - Le paramètre `symbol` est reçu mais `"ERROR"` est utilisé à la place.
  - → Utiliser le paramètre dans le record.

- [ ] **BACK-16** — Incohérence du timestamp dans `AssetSyncJob`
  - 📁 `AssetSyncJob.java` L69 vs L86
  - HYPE utilise `ad.getLastRefresh()`, INVE-B utilise `System.currentTimeMillis()`.
  - → Harmoniser la source du timestamp.

- [ ] **BACK-17** — `@GeneratedValue(strategy = AUTO)` avec Postgres
  - 📁 `AssetDaily.java` L17, `AssetSnapshot.java` L18
  - `AUTO` peut générer une table de séquences séparée. Moins performant.
  - → `@GeneratedValue(strategy = GenerationType.IDENTITY)`

- [ ] **BACK-18** — `@Bean` statique et redondant sur `RestClient.Builder`
  - 📁 `DashboardApplication.java` L13-18
  - Spring Boot auto-configure déjà un `RestClient.Builder`. Ce bean est redondant.
  - → Supprimer, ou le garder mais configurer timeouts dedans.

- [ ] **BACK-19** — `@Transactional` en double sur les deletes
  - 📁 `AssetDailyRepository.java` L24, `AssetSyncJob.java` L95
  - Double `@Transactional` repository + appelant. Fonctionnel mais confus.
  - → Garder uniquement au niveau du job.

### Frontend

- [ ] **FRONT-05** — Duplication massive des composants Chart
  - 📁 `price-chart.ts` + `daily-chart.ts`
  - ~80% de code identique (constructor/effect/createChart/updateChart, config tooltip/scales). CSS aussi quasi identique.
  - → Créer un `BaseChart` abstrait ou un composant configurable unique.

- [ ] **FRONT-06** — `formatNumber`/`formatTime` en propriétés au lieu de Pipes
  - 📁 `hype.ts` L25-26, `inveb.ts` L20-21, `asset-main-card.ts` L14-15
  - Dupliqué 3 fois. Devrait être des Pipes Angular réutilisables.
  - → Créer `FormatNumberPipe` et `FormatTimePipe` dans `shared/pipes/`

- [ ] **FRONT-09** — `window.devicePixelRatio` accès direct au DOM
  - 📁 `daily-chart.ts` L77, `price-chart.ts` L76
  - Pas bloquant pour une SPA, mais problème SSR/tests.
  - → Injecter via un service ou `isPlatformBrowser()`

- [ ] **FRONT-10** — Magic numbers dans les composants
  - 📁 `hype.ts` L171 (`180000`), `inveb.ts` L39 (`180000`), `hype-flux-chart.ts` L61 (`86400000`), `hype-supply-distribution.ts` L45 (`1000000000`)
  - → Extraire en constantes nommées.

### Fonctionnalités planifiées

- [ ] **FEAT-01** — Chart volume + Open Interest de HYPE sur 30 jours
  - Nécessite côté backend : persister les données volume/OI en base quotidiennement via le job de sync.
  - Côté frontend : créer le composant chart (30j) une fois les données disponibles.
  - → Commencer par le modèle DB + collecte, puis le chart.

- [ ] **FEAT-02** — Chart daily pour Investor AB
  - 📁 `frontend/src/app/features/crypto/inveb/`
  - Ajouter un graphique d'évolution journalière (même logique que le daily chart de HYPE).
  - → Réutiliser/adapter le composant `daily-chart` existant.

- [ ] **FEAT-03** — Metrics cards pour Investor AB
  - 📁 `frontend/src/app/features/crypto/inveb/`
  - Ajouter les cartes de métriques clés (même esprit que les cards HYPE).
  - → Définir quelles métriques afficher, puis créer les composants.

- [ ] **FEAT-04** — Refresh automatique des prix sur le dashboard
  - Les prix affichés ne se mettent pas à jour sans rechargement manuel.
  - → Implémenter un polling périodique ou un mécanisme de refresh auto (interval RxJS).

---

## 🔵 INFO

- [ ] **TEST-01** — Couverture backend quasi nulle
  - 📁 `DashboardApplicationTests.java` — seul `contextLoads()`
  - Zéro test pour les services, calculs, providers, jobs.
  - → Priorité : `HypeService`, `AssetSyncJob`, puis les clients API.

- [ ] **TEST-02** — Tests frontend ultra-minimaux
  - 14 fichiers `.spec.ts`, tous `should create` sauf `dashboard.spec.ts`.
  - `price-chart` et `hype.ts` n'ont pas de fichier spec.
  - → Tester les `computed()` dans `hype.ts` et `inveb.ts`.

- [ ] **QUAL-04** — `<button routerLink>` au lieu de `<a>`
  - 📁 `navbar.html` L8, `asset-dashboard-card.html` L2
  - → Utiliser `<a routerLink="...">` pour les liens de navigation.

- [ ] **QUAL-05** — `lang="en"` sur une app avec contenu mixte FR/EN
  - 📁 `index.html` L2
  - → Mettre `lang="fr"`

- [ ] **A11Y-01** — Boutons/SVG sans attributs ARIA
  - Boutons navbar sans `aria-label`, SVGs décoratifs sans `aria-hidden="true"`, canvas chart sans fallback.
  - → `aria-label` sur les boutons, `aria-hidden="true"` sur les SVG décoratifs.

- [ ] **INFRA-06** — Dépendance `security-test` sans `security`
  - 📁 `pom.xml` L72-84
  - `spring-boot-starter-security-test` inclus mais `security` est commenté.
  - → Retirer `security-test` tant que Security n'est pas activé.

- [ ] **INFRA-08** — Dockerfile sans user non-root
  - 📁 `Dockerfile`
  - → Ajouter `addgroup/adduser` + `USER app`

- [ ] **INFRA-09** — Pas de cache des dépendances Maven dans le Dockerfile
  - 📁 `Dockerfile` L4-6
  - Chaque changement de code redownload toutes les dépendances.
  - → `COPY pom.xml` → `mvn dependency:go-offline` → `COPY src`

- [ ] **INFRA-10** — Dossier `target/` résiduel à la racine
  - Build lancé au mauvais endroit. Gitignored mais du bruit.
  - → Supprimer manuellement.

- [ ] **INFRA-11** — `application-prod.yml` non gitignored
  - 📁 `.gitignore` L35-38
  - Pas de secrets dedans actuellement, mais risque si quelqu'un en ajoute.
  - → Gitignorer ou ajouter un commentaire d'avertissement.

- [ ] **INFRA-12** — Burger menu non fonctionnel
  - 📁 `navbar.html` L27-31
  - Bouton menu présent sans click handler ni dropdown.
  - → Implémenter ou retirer.

---

## 📊 Résumé

| Sévérité | Restant |
|----------|---------|
| 🔴 Critique | 1 |
| 🟠 Élevé | 10 |
| 🟡 Moyen | 16 |
| 🔵 Info | 9 |
