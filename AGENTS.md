# Dashboard — AGENTS (monorepo)

Coordination cross-stack. Détails front → [`frontend/AGENTS.md`](frontend/AGENTS.md) · détails back → [`backend/AGENTS.md`](backend/AGENTS.md).

**Profil** : développeur en pré-apprentissage — priorité pédagogie, pas à pas, explications ; code par l'agent seulement sur demande explicite (voir `monorepo.mdc`).

## Déploiement

| Partie | Cible | Notes |
|--------|-------|-------|
| Frontend | Vercel | SPA, `vercel.json` rewrite → `index.html` |
| Backend | VPS Infomaniak 4 Go | Docker + Postgres + Watchtower (pull horaire) |

Pipeline back : push `backend/**` sur `master` → GitHub Actions → Docker Hub → Watchtower sur le VPS.

## Contrat API

Base prod : `https://api.dokkcorp.ch` · dev : `http://localhost:8080`

| Méthode | Chemin | Réponse |
|---------|--------|---------|
| GET | `/api/dashboard/hype` | `HypeDto` |
| GET | `/api/dashboard/inveb` | `InveBDto` |

CORS : `${app.cors.allowed-origins}` (env `CORS_ORIGINS` en prod).

## Commandes utiles

```bash
cd frontend && npm start          # ng serve
cd frontend && ng test
cd backend && mvn spring-boot:run
cd backend && mvn test
```

## Checklist avant prod

- [ ] Variables env prod (Postgres, API keys, `CORS_ORIGINS`, `BLOCKCHAIN_RPC_URL`)
- [ ] `environment.prod.ts` pointe vers l’API prod
- [ ] CORS aligné domaine Vercel ↔ API
- [ ] `mvn test` + `ng test` passent
- [ ] Image Docker back testée localement si changement infra

## Journal (cross-stack)

Entrées pour décisions ou changements touchant **front et back** (contrat API, déploiement, nommage partagé).

---

### 2026-05-21 — Mise en place contexte Cursor

- **Contexte** : Optimisation Cursor avant passage Pro (règles `.mdc`, journaux, `.cursorignore`).
- **Décisions** : Règles scindées monorepo / angular / spring ; `AGENTS.md` versionnés par périmètre.
- **Fichiers clés** : `.cursor/rules/*.mdc`, `AGENTS.md`, `.cursorignore`.
- **Dette / TODO** : Phase 2 — MCP Angular (`ng mcp`), règle explicite pour lancer les tests.
- **Pièges** : Ne pas dupliquer le journal détaillé ici — utiliser `frontend/` ou `backend/AGENTS.md`.
