# Frontend Angular — AGENTS

Journal et carte du projet front. Règles détaillées : [`.cursor/rules/angular-frontend.mdc`](../.cursor/rules/angular-frontend.mdc).

## Carte du projet

```
src/app/
├── core/           → navbar, footer, format-number, format-dates
├── features/
│   ├── dashboard/  → page d’accueil (/)
│   ├── crypto/hype/ → dashboard HYPE (/hype)
│   └── stocks/inveb/ → dashboard INVE-B (/inveb)
└── shared/components/ → cartes, charts, background
```

### Routes

| Path | Composant |
|------|-----------|
| `/` | `Dashboard` |
| `/hype` | `Hype` |
| `/inveb` | `Inveb` |

### API consommée

- `GET ${environment.apiUrl}/api/dashboard/hype` — polling ~180s (`Hype`)
- `GET ${environment.apiUrl}/api/dashboard/inveb` — polling 60s (`Inveb`)
- `Dashboard` : fetch one-shot via `toSignal`

### Conventions

- Standalone, signals, control flow `@if` / `@for`
- Tailwind : tokens `dark-*`, `copper-*`
- Tests : Vitest, `ng test`

## Journal

Nouvelles entrées **en tête** (plus récent en premier). Format : Contexte | Décisions | Fichiers clés | Dette/TODO | Pièges.

---

### 2026-05-21 — Initialisation AGENTS.md (exemple)

- **Contexte** : Création du journal front dans le cadre de l’optimisation Cursor.
- **Décisions** : Documenter routes, endpoints et structure `core` / `features` / `shared` ; HTTP à terme dans des services par asset.
- **Fichiers clés** : `app.routes.ts`, `features/crypto/hype/hype.ts`, `features/stocks/inveb/inveb.ts`.
- **Dette / TODO** : Extraire HTTP de `Hype` / `Inveb` vers services injectables ; typer les DTOs API ; migrer `@Input` sur `PriceChart` / `DailyChart`.
- **Pièges** : Oublier `chart.destroy()` et le cleanup des `setInterval` / `timer` provoque des fuites mémoire.
