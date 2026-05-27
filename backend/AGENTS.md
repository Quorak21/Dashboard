# Backend Spring — AGENTS

Carte du projet back. Règles détaillées : [`.cursor/rules/spring-backend.mdc`](../.cursor/rules/spring-backend.mdc).

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

- **dev** : `application.yml` + `compose.dev.yaml` (démarrage du Postgres local de dev via `docker compose -f compose.dev.yaml up -d`)
- **prod** : `docker-compose.yml` (production, 3 services), variables `POSTGRES_*`, `COINGECKO_API_KEY`, `FMP_API_KEY`, `BLOCKCHAIN_RPC_URL`, `CORS_ORIGINS`
- JVM prod (compose) : `-Xmx1g -Xms512m` (JAVA_TOOL_OPTIONS)

### Symboles métier

- `"HYPE"` — crypto
- `"INVE-B"` — actions (FMP : `INVE-B.ST`)

## Suivi dette technique

Voir [`code_review.md`](../code_review.md) et [`journal.md`](../journal.md) à la racine.
