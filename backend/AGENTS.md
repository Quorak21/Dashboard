# Backend Spring — AGENTS

Journal et carte du projet back. Règles détaillées : [`.cursor/rules/spring-backend.mdc`](../.cursor/rules/spring-backend.mdc).

## Carte du projet

```
com.dokkcorp.dashboard/
├── controller/DashboardController   → /api/dashboard
├── features/crypto/hype/            → HypeService, HypeDto
├── features/stocks/investorab/      → InveBService, InveBDto
├── jobs/AssetSyncJob                → sync 10 min, snapshot minuit, cleanup
├── model/entity/                    → AssetDaily, AssetSnapshot
├── repository/
└── providers/                       → CoinGecko, FMP, Hyperliquid, blockchain
```

### Endpoints

| Méthode | Chemin | Service |
|---------|--------|---------|
| GET | `/api/dashboard/hype` | `HypeService.getLastHypeData()` |
| GET | `/api/dashboard/inveb` | `InveBService.getLastInveBData()` |

### Jobs planifiés (`AssetSyncJob`)

| Cron | Action |
|------|--------|
| `0 0/10 * * * ?` | `autoSync()` — refresh HYPE + INVE-B |
| `0 0 0 * * ?` UTC | Snapshot quotidien en base |
| `0 0 0 * * SUN` | `cleanDB()` — rétention |

### Profils & config

- **dev** : `application.yml`, Docker Compose Postgres local possible
- **prod** : `application-prod.yml`, variables `POSTGRES_*`, `COINGECKO_API_KEY`, `FMP_API_KEY`, `BLOCKCHAIN_RPC_URL`, `CORS_ORIGINS`
- JVM prod (compose) : `-Xmx1g -Xms512m`

### Symboles métier

- `"HYPE"` — crypto
- `"INVE-B"` — actions (FMP : `INVE-B.ST`)

## Journal

Nouvelles entrées **en tête** (plus récent en premier). Format : Contexte | Décisions | Fichiers clés | Dette/TODO | Pièges.

---

### 2026-05-21 — Initialisation AGENTS.md (exemple)

- **Contexte** : Création du journal back dans le cadre de l’optimisation Cursor.
- **Décisions** : Un seul controller REST ; providers retournent `null` en erreur ; services dégradent vers cache ou `*.error()`.
- **Fichiers clés** : `DashboardController.java`, `HypeService.java`, `InveBService.java`, `AssetSyncJob.java`.
- **Dette / TODO** : Virtual threads non configurés ; tests au-delà de `contextLoads` ; `BigDecimal` mentionné en TODO sur entités.
- **Pièges** : Dupliquer la logique de fetch dans les jobs au lieu d’appeler `*Service.getData()` ; hardcoder des clés API.
