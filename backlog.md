# Dashboard — Backlog

**Statut produit** : Epic 6 livré (6 actifs registry, overview dynamique, navigation complète). En attente de déploiement prod.

| Fichier | Rôle |
|---------|------|
| `backlog.md` | **Tâches en cours** (dettes, bugs, chantiers, deferred) + **FF** (idées et features futures) |
| `journal.md` | Jalons et tâches **terminées** |

> **Convention tickets** : `ID` — **Titre** : description. Gravité **CRITIQUE → ÉLEVÉ → MOYEN → INFO** pour les tâches en cours. Préfixe **DEF-** pour le travail différé (code review / rétro). Préfixe **FF-** pour les futurs features. Résolu → retirer d'ici + **une ligne** dans `journal.md`. Jamais de secrets en clair.

---

## Tâches en cours

Bugs, risques, dettes et chantiers actifs. Odin y ajoute de manière autonome dès qu'un souci est repéré ; toi aussi quand tu dis « on fera plus tard ».

### CRITIQUE

Pas de tâche critique pour le moment.

### ÉLEVÉ

- **DEF-01** — **Routage paramétré unique Angular** : remplacer les wrappers par actif (`O`, `Iii`, `Infr`…) par une route unique `/asset/:assetId` dans `app.routes.ts` et supprimer les fichiers redondants. (Rétro Epic 6)
- **DEF-02** — **Factorisation des subscriptions dashboard** : remplacer les blocs RxJS copiés-collés dans `dashboard.ts` par une boucle / map `assetId → signal` (refresh, unsubscribe, polling 3 min). (Rétro Epic 6 + deferred 6-5)

### MOYEN

- **DEF-03** — **Overview dashboard 100 % registry-driven** : titres, devises et cartes depuis `getRegisteredAssets()` / métadonnées API — finir de retirer le hardcode résiduel dans le template. (Deferred 6-1, 6-3, 6-5)
- **DEF-04** — **Renommage `toast.service.ts`** : aligner le nom de fichier sur la convention Angular (`toastService.ts` → `toast.service.ts`) et mettre à jour tous les imports. (Deferred 4-1, 6-6)
- **DEF-05** — **Fixture registry tests isolée** : ne plus dupliquer `assets-registry.yml` prod dans les tests — utiliser `assets-registry-fixture.yml` partout. (Deferred 6-2, 6-3)
- **DEF-06** — **Respect interval/offset YAML dans `AssetSyncJob`** : le cron ignore encore `sync.intervalMinutes` / `offsetMinutes` du registre. (Deferred 2-2, 2-4)
- **DEF-07** — **Rate limiting batch FMP** : ajouter un délai ou throttle entre actifs dans la boucle `@Scheduled` pour éviter de saturer le quota free tier. (Deferred 2-2, 2-4)
- **DEF-08** — **Verrou pendant fetch réseau** : `ConfigurableAssetService.syncPrice` tient un lock pendant les appels provider — risque de contention ; extraire fetch hors synchronized. (Deferred 1-4, 2-4, 3-3)
- **DEF-09** — **Clock injectable** : remplacer `System.currentTimeMillis()` par un `Clock` bean pour tests déterministes (cache age, market hours). (Deferred 2-1, 3-3)

### INFO

- **DEF-10** — **Titres graphiques EN dans UI FR** : « Annual Performance », « Daily Live » dans `asset-page.html` — harmoniser la langue affichée. (Deferred 4-3)
- **DEF-11** — **Format devise non localisé** : `dividend-card` concatène `${value} ${currency}` au lieu d'utiliser `Intl` / locale. (Deferred 4-2)
- **DEF-12** — **MarketHoursGuard sans jours fériés** : acceptable pour l'instant ; calendrier boursier complet = chantier futur si besoin. (Deferred 1-3, 2-1)
- **DEF-13** — **Observabilité Micrometer** : métriques SLF4J seulement via `ProviderCallMetrics` — suffisant pour le VPS perso. (Deferred 2-3)
- **NOTE-O-01** — **Fundamentaux Realty Income (O) vérifiés** : données YAML contrôlées manuellement le 2026-07-06 — **OK jusqu'au prochain trimestre** (prochaine revue attendue ~oct. 2026, publication T3).

---

## Dette acceptée

Dette assumée pour un portfolio perso — pas de ticket actif, documentée pour mémoire.

- **BACK-08** — Types `String` pour des valeurs numériques en base : migration VARCHAR → DOUBLE trop risquée en prod ; DTOs déjà en `Double`.
- **BACK-17** — `@GeneratedValue(strategy = AUTO)` avec Postgres : schéma prod + `ddl-auto: validate` — pas de migration sans gain.
- **BACK-10** — Pas d'index DB explicites (`symbol`, `lastRefresh`, `day`) : ~7 actifs, tables petites (rétention 7j / 1 an) — gain négligeable sur ce VPS.
- **SEC-05** — Aucune authentification sur l'API : données publiques en lecture seule, portfolio d'apprentissage.
- **FRONT-05** — Charts non fusionnés en `BaseChart` : chaque graphique a son comportement visuel propre (flux, doughnut, live…).
- **FRONT-10** — Quelques magic numbers isolés (`180000`, `86400000`…) — sens évident en contexte.
- **SEC-08** — Mot de passe prod historique (`dokksecret`) non rotationné : DB non exposée (bind `127.0.0.1`), secret dans l'historique git seulement.

---

## FF

Idées, évolutions et améliorations secondaires — tri libre.

- **FF-01** — **Bulle d'aide sur les métriques fondamentales** : icône info (tooltip / popover) à côté de chaque label dans `fundamentals-card` pour rappeler ce qu'est la métrique (FFO, AFFO, payout ratio…) au cas où tu oublies entre deux trimestres.
- **FF-02** — **Scrape prix SIX / justETF** : Epic 5 annulé — réévaluer si quota FMP insuffisant (CHDIV, QQQE).
- **FF-03** — **Reload config YAML sans redéploiement** : actuator refresh ou hot-reload du registre actifs.
- **FF-04** — **Micrometer / Prometheus** : métriques provider au-delà des logs SLF4J si le VPS grossit.

---

## Résumé

| Sévérité | Restant |
|----------|---------|
| CRITIQUE | 0 |
| ÉLEVÉ | 2 |
| MOYEN | 7 |
| INFO | 5 |
