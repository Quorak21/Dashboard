# 📋 Dashboard — Backlog dette technique

> **Mise à jour** : 2026-06-04 · **Tâches actives** : 13
>
> Tâches résolues → `journal.md`. Jamais de secrets en clair.

---

## 🔴 CRITIQUE
*(Plus de tâche critique active !)*

---

## 🟠 ÉLEVÉ

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

- [ ] **FRONT-12** — HYPE : résumé d’erreur = champs `null` (pas de zéros factices)
  - Convention : en cas d’échec API / état dégradé, `HypeSummaryDto` expose des **`null`** pour marquer « pas de donnée », à distinguer d’une vraie valeur `0`.
  - 📁 Back : `HypeDto.java`, `HypeSummaryDto.java`, factory `error()` — garder cette convention alignée sur l’agrégat.
  - → Front (quand refactor HYPE branché) : lire `summaryDto` / `summary`, UI « indisponible » si `null`, optional chaining ; ne pas traiter `0` comme erreur pour le bloc summary.

---

## 🟡 MOYEN

### Backend

- [ ] **BACK-21** — Découper le cache/récupération HYPE par thème pour isoler les pannes partielles
  - 📁 `HypeService.java`
  - Cache et récupération actuels sont globaux : si une source échoue, le fallback peut dégrader tout l'agrégat.
  - → Séparer par thèmes (`summary`, `timedData`, `supply`, `blockchain`, `hlp`, `valuation`) avec cache/fallback dédiés pour ne dégrader que le secteur en erreur.
  - Priorisation : à traiter après **FRONT-12** (gestion UI du dégradé par section).

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

- [ ] **BACK-22** — Corriger la propagation du symbole dans `InveBDto.error()`
  - 📁 `InveBService.java` L87
  - `InveBService` propage `"ERROR"` comme symbole lors d'une exception dans `InveBDto.error()`.
  - → Utiliser le bon symbole `"INVE-B"` lors de l'appel.

### Frontend

- [ ] **FRONT-05** — Duplication massive des composants Chart
  - 📁 `price-chart.ts` + `daily-chart.ts`
  - ~80% de code identique (constructor/effect/createChart/updateChart, config tooltip/scales). CSS aussi quasi identique.
  - → Créer un `BaseChart` abstrait ou un composant configurable unique.

- [ ] **FRONT-10** — Magic numbers dans les composants
  - 📁 `hype.ts` L171 (`180000`), `inveb.ts` L39 (`180000`), `hype-flux-chart.ts` L61 (`86400000`), `hype-supply-distribution.ts` L45 (`1000000000`)
  - → Extraire en constantes nommées.

### Fonctionnalités planifiées

- [ ] **FEAT-01** — Chart volume + Open Interest de HYPE sur 30 jours
  - Nécessite côté backend : persister les données volume/OI en base quotidiennement via le job de sync.
  - Côté frontend : créer le composant chart (30j) une fois les données disponibles.
  - → Commencer par le modèle DB + collecte, puis le chart.

- [ ] **FEAT-04** — Refresh automatique des prix sur le dashboard
  - Les prix affichés ne se mettent pas à jour sans rechargement manuel.
  - → Implémenter un polling périodique ou un mécanisme de refresh auto (interval RxJS).

---

## 🔵 INFO

*(Plus de tâche info active !)*

---

## 💀 MÊME PAS EN RÊVE (Dette technique acceptée ad aeternam)

- **BACK-08** — Types `String` pour des valeurs numériques partout
  - *Pourquoi ?* : Trop risqué de migrer la base de données en production de VARCHAR vers DOUBLE PRECISION. On a déjà corrigé tous les DTO en `Double` (ou presque !), le reste tiendra très bien comme ça.
- **BACK-10** — Pas d'index DB explicites (`symbol` + `lastRefresh` / `day`)
  - *Pourquoi ?* : Peu d'actifs (2 aujourd'hui, ~10 max), tables petites (rétention 7j / 1 an) — gain perf négligeable sur ce VPS. Bon réflexe à réutiliser sur un prochain projet si volumes ou requêtes lourdes.
- **SEC-05** — Aucune authentification sur l'API
  - *Pourquoi ?* : C'est un portfolio d'apprentissage personnel, pas besoin d'ajouter une usine à gaz comme Spring Security pour des données publiques en lecture seule.

---

## 📊 Résumé

| Sévérité | Restant |
|----------|---------|
| 🔴 Critique | 0 |
| 🟠 Élevé | 2 |
| 🟡 Moyen | 11 |
| 🔵 Info | 0 |
