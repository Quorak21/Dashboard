# 📓 Journal de bord — Dashboard

Historique des tâches et de la dette technique résolues sur le monorepo (Spring Boot + Angular).

- **Tâches réalisée depuis l'implémentation du workflow : 23**

---

- **BACK-03** — Extraire les calculs de `HypeService.mapToDto()` vers `HypeCalculator`
  - *Description* : migration des calculs principaux (`timedData`, `supply`, `blockchain`, `hlp`, `valuation`) dans `HypeCalculator`, avec `HypeService` recentré sur l'orchestration et délégation des calculs.

- **SEC-01** — Clés API en clair
  - *Description* : Fichier `application-dev.yml` sécurisé et bien protégé dans le `.gitignore` racine. Aucun risque d'exposition des clés CoinGecko, Alchemy et FMP.

- **SEC-02** — Mots de passe Postgres en clair dans les docker-compose
  - *Description* : Identifiants de dev local (masqués pour la sécurité) validés comme locaux et non-critiques.

- **SEC-03** — `spring.web.error.include-message` hérité en prod
  - *Description* : Configuration `spring.web.error.include-message: never` ajoutée dans `application-prod.yml` pour empêcher la fuite de stack traces en production.

- **SEC-04** — Actuator exposé sans protection
  - *Description* : Restriction d'exposition configurée dans `application-prod.yml` avec `management.endpoints.web.exposure.include: health` pour n'exposer publiquement que la santé du système.

- **DB-01** — `ddl-auto: update` en production
  - *Description* : Surcharge configurée à `ddl-auto: validate` dans `application-prod.yml` pour bloquer les altérations automatiques et destructrices du schéma de base en production par Hibernate.

- **BACK-01** — État mutable dans les singletons Spring (Thread-safety)
  - *Description* : Cache de `HypeService` et `InveBService` encapsulé dans des `AtomicReference<T>`, et variables partagées d'historique marquées `volatile` pour éviter les race conditions et garantir la cohérence en cas d'accès parallèles.

- **FRONT-03** — `subscribe()` manuels sans cleanup
  - *Description* : Ajout de `takeUntilDestroyed(this.destroyRef)` dans les pipes d'appels API de `hype.ts` et `inveb.ts` pour détruire les souscriptions à la destruction des composants Angular (évite les fuites de mémoire).

- **FRONT-11** — Import inutilisé dans le frontend
  - *Description* : Suppression de l'import inutile `import { environment }` de `hype.ts`.

- **INFRA-05** — H2 database embarquée en production
  - *Description* : Modification du scope de la dépendance `h2` de `runtime` à `test` dans le `pom.xml` du backend. La base de dev n'est plus packagée dans le JAR de production.

- **INFRA-07** — Risque de confusion avec compose.yaml
  - *Description* : Renommage du fichier compose de dev `compose.yaml` ➔ `compose.dev.yaml` pour éviter les lancements dev/prod accidentels. Documentation mise à jour dans `backend/AGENTS.md`.

- **QUAL-01** — Imports `java.util` inline dans HypeService
  - *Description* : Nettoyage des écritures absolues `java.util.ArrayList` et `java.util.Collections.reverse` par des imports standards en haut de fichier.

- **QUAL-03** — SVG icons inline au lieu de lucide-angular
  - *Description* : Remplacement des icônes SVG en dur par des balises `<lucide-icon>` avec les icônes de la librairie officielle (`Zap`, `TrendingUp`, `TrendingDown`, `Landmark`, `Boxes`, `Clock` et `Flame`) sur le Dashboard, l'Asset Card, et la Hype Burn Card.

- **QUAL-02** — Devises `$` hardcodées dans les callbacks chart
  - *Description* : Remplacement du symbole `$` en dur dans le tooltip et l'axe Y du `daily-chart` par l'input `currency()`, rendant le composant agnostique de la devise.

- **INFRA-03** — Tag Docker unique `latest` sans version
  - *Description* : Ajout d'un second tag `${{ github.sha }}` dans `deploy.yml` en plus de `latest`, permettant le rollback vers un commit précis via Docker Hub.

- **INFRA-04** — `@CrossOrigin` avec SpEL potentiellement mal résolu
  - *Description* : Non bloquant avec une seule origine. Solution identifiée (`WebMvcConfigurer` + `CorsRegistry` avec split) pour le jour où un second front sera ajouté.

- **FRONT-04** — `catchError(() => of(null))` avale les erreurs silencieusement
  - *Description* : Création d'un `ToastService` (signal + timer) et d'un composant `ToastComponent` affiché globalement via `app.component`. Le `catchError` du service API déclenche maintenant un toast d'erreur. Ajout de fallbacks `"-"` sur l'`AssetMainCard` (prix, volume, market cap) quand les données sont `null`.

- **FRONT-08** — Pas de route wildcard `**` pour les 404
  - *Description* : Ajout d'une route catch-all `{ path: '**', redirectTo: '/' }` en dernière position dans `app.routes.ts`, redirigeant les URL inconnues vers le dashboard.

- **FRONT-07** — `standalone: true` explicite inutile (défaut Angular 19+)
  - *Description* : Retrait de `standalone: true` dans `hype.ts`, `hype-burn-card.ts` et `hype-flux-chart.ts`. Redondant depuis Angular 19 où standalone est le défaut.

- **BACK-15** — Duplication du calcul `feesDaily`
  - *Description* : Extraction du taux de fees `0.00022` dans une constante `HYPE_FEE_RATE` en haut de `HypeService`, utilisée aux deux endroits du calcul (`getData()` et `mapToDto()`).

- **BACK-12** — Valeurs magiques hardcodées partout
  - *Description* : Création de `HypeConstants.java` centralisant `MAX_SUPPLY`, `MAX_SUPPLY_BD` et `FEE_RATE`. Remplacement des constantes locales dans `HypeService`, `HyperliquidClient` et `BlockChainClient` par des références à `HypeConstants`.

- **INFRA-01** — `JAVA_OPTS` ignoré dans le Dockerfile
  - *Description* : Ajout de `JAVA_TOOL_OPTIONS` avec `-XX:MaxRAMPercentage=75.0` dans le Dockerfile (la JVM lit cette variable nativement). Suppression de la ligne `JAVA_OPTS` inutile dans `docker-compose.yml`. Le heap s'adapte désormais automatiquement à la limite mémoire du conteneur.

- **BACK-02** — Pas de `@ControllerAdvice` / gestion d'erreur globale
  - *Description* : Package `exception/` avec `ApiErrorResponse` (message seul) et `GlobalExceptionHandler` (`@RestControllerAdvice`) : log complet côté serveur, réponse HTTP 500 avec message générique sans stack trace au client.