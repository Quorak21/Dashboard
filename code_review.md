# 📋 Dashboard — Backlog dette technique

> **Mise à jour** : 2026-06-05 · **Tâches actives** : 1
>
> Tâches résolues → `journal.md`. Jamais de secrets en clair.

---

## 🔴 CRITIQUE
*(Plus de tâche critique active !)*

---

## 🟠 ÉLEVÉ

*(Plus de tâche élevée active !)*

---

## 🟡 MOYEN

*(Plus de tâche moyenne active !)*

---

## 🔵 INFO

### Backend

- [ ] **BACK-24** — Pas de garde anti-« thundering herd » sur le cold-start du cache
  - 📁 `HypeService.getLastHypeData()` / `InveBService.getLastInveBData()`
  - Tant que le cache (`AtomicReference`) est vide (boot avant le 1er cron), chaque requête publique déclenche un `getData()` complet (6+ appels externes + insert DB). Plusieurs requêtes concurrentes lancent autant de `getData()` en parallèle (pas de single-flight) → appels externes redondants + lignes `AssetDaily` dupliquées.
  - Impact réel **faible** (mono-utilisateur, fenêtre = quelques secondes au boot), d'où INFO.
  - → Option simple : un `synchronized`/lock léger ou un flag `loading` pour mutualiser le 1er remplissage. À garder en tête, pas urgent.

---

## 💀 MÊME PAS EN RÊVE (Dette technique acceptée ad aeternam)

- **BACK-08** — Types `String` pour des valeurs numériques partout
  - *Pourquoi ?* : Trop risqué de migrer la base de données en production de VARCHAR vers DOUBLE PRECISION. On a déjà corrigé tous les DTO en `Double` (ou presque !), le reste tiendra très bien comme ça.
- **BACK-17** — `@GeneratedValue(strategy = AUTO)` avec Postgres
  - *Pourquoi ?* : Schéma prod déjà en place + `ddl-auto: validate` — risque de boot en échec sans gain réel sur ce volume. À faire dès le départ sur le prochain projet (`GenerationType.IDENTITY`).
- **BACK-10** — Pas d'index DB explicites (`symbol` + `lastRefresh` / `day`)
  - *Pourquoi ?* : Peu d'actifs (2 aujourd'hui, ~10 max), tables petites (rétention 7j / 1 an) — gain perf négligeable sur ce VPS. Bon réflexe à réutiliser sur un prochain projet si volumes ou requêtes lourdes.
- **SEC-05** — Aucune authentification sur l'API
  - *Pourquoi ?* : C'est un portfolio d'apprentissage personnel, pas besoin d'ajouter une usine à gaz comme Spring Security pour des données publiques en lecture seule.
- **FRONT-05** — Duplication massive des composants Chart
  - *Pourquoi ?* : Chaque chart a son identité visuelle et son comportement (flux symétrique, doughnut supply, prix live vs daily…). La fusion en `BaseChart` appauvrirait le rendu pour un gain de maintenance marginal sur un petit nombre de composants.
- **FRONT-10** — Magic numbers dans les composants
  - *Pourquoi ?* : Quelques littéraux isolés (`180000`, `86400000`, `1_000_000_000`), sens évident en contexte — pas la peine d'extraire des constantes pour si peu.

---

## 📊 Résumé

| Sévérité | Restant |
|----------|---------|
| 🔴 Critique | 0 |
| 🟠 Élevé | 0 |
| 🟡 Moyen | 0 |
| 🔵 Info | 1 |
