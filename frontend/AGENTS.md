# Frontend Angular — AGENTS

Carte du projet front. Règles détaillées : [`.cursor/rules/angular-frontend.mdc`](../.cursor/rules/angular-frontend.mdc).

## Carte du projet

```
src/app/
├── core/           → navbar, footer, format-number, format-dates
├── features/
│   ├── dashboard/  → page d'accueil (/)
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

## Suivi dette technique

Voir [`code_review.md`](../code_review.md) et [`journal.md`](../journal.md) à la racine.
