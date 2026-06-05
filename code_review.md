# 📋 Dashboard — Backlog dette technique

> **Mise à jour** : 2026-06-05 · **Tâches actives** : 5
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

### Backend

- [ ] **BACK-21** — Découper le cache/récupération HYPE par thème pour isoler les pannes partielles
  - 📁 `HypeService.java`
  - Cache et récupération actuels sont globaux : si une source échoue, le fallback peut dégrader tout l'agrégat.
  - → Séparer par thèmes (`summary`, `timedData`, `supply`, `blockchain`, `hlp`, `valuation`) avec cache/fallback dédiés pour ne dégrader que le secteur en erreur.
  - Priorisation : **FRONT-12 résolu** — UI HYPE gère déjà le dégradé section par section (`null` → `-` / empty state). Prochaine étape : isoler le fallback côté back.

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
  - 📁 `InveBService.java` L87 · `InveBDto.java`
  - `InveBService` propage `"ERROR"` comme symbole lors d'une exception dans `InveBDto.error()`.
  - → Utiliser le bon symbole `"INVE-B"` lors de l'appel.
  - → Aligner `InveBDto.error()` sur la convention HYPE : champs financiers en `null` (pas de `0.0` factice) pour que le front dégrade proprement.

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
| 🟡 Moyen | 5 |
| 🔵 Info | 0 |
