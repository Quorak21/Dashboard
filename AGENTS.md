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

## Suivi dette technique

Backlog actif : [`code_review.md`](code_review.md) · Historique résolu : [`journal.md`](journal.md)

Voir politique complète dans `.cursor/rules/monorepo.mdc`.
