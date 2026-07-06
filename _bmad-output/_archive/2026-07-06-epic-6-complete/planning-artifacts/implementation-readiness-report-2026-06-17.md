---
stepsCompleted: [1, 2, 3, 4, 5, 6]
workflowType: implementation-readiness
project_name: Dashboard
user_name: Dokk
date: '2026-06-17'
status: complete
assessor: bmad-check-implementation-readiness
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-Dashboard-2026-06-17/prd.md
  - _bmad-output/planning-artifacts/prds/prd-Dashboard-2026-06-17/addendum.md
  - _bmad-output/planning-artifacts/prds/prd-Dashboard-2026-06-17/.decision-log.md
  - _bmad-output/planning-artifacts/architecture.md
  - _bmad-output/planning-artifacts/epics.md
  - docs/project-context.md
  - _bmad-output/planning-artifacts/research/spike-scrape-ubs-report-2026-06-17.md
---

# Implementation Readiness Assessment Report

**Date:** 2026-06-17  
**Project:** Dashboard — Extension multi-actifs (PRD v2.1)  
**Assessor:** BMAD Implementation Readiness workflow

---

## 1. Document Discovery

### Inventaire

| Type | Fichier(s) retenu(s) | Format | Statut |
|------|----------------------|--------|--------|
| **PRD** | `prds/prd-Dashboard-2026-06-17/prd.md` (+ addendum, decision-log) | Dossier sharded | ✅ Complet, `final` v2.1 |
| **Architecture** | `architecture.md` | Document entier | ✅ Complet, 15 ADR |
| **Epics & Stories** | `epics.md` | Document entier | ✅ Complet, 6 epics / 28 stories |
| **UX Design** | — | — | ⚠️ Absent |
| **Project context** | `docs/project-context.md` | Référence | ✅ |
| **Research** | `research/spike-scrape-ubs-report-2026-06-17.md` | Spike | ✅ Gate D-21 |

### Doublons

Aucun conflit whole vs sharded : un seul PRD (dossier), une architecture, un epics.

### Documents exclus

- PRD v1 obsolète (supersédé v2.1)
- Research spike providers (contexte, non bloquant)

---

## 2. PRD Analysis

### Functional Requirements (18)

| ID | Résumé |
|----|--------|
| FR-1 | Registre actifs YAML multi-provider |
| FR-2 | `GET /api/dashboard/{assetId}` contrat unifié |
| FR-3 | Persistance AssetDaily / AssetSnapshot par db-symbol |
| FR-4 | Dividendes config + `estimatedYield` calculé backend |
| FR-5 | Fondamentaux config + alerte `updatedAt` > 90 j |
| FR-6 | Workflow MAJ config LLM hors runtime |
| FR-7 | Scrape Yahoo chart CHDIV + QQQE |
| FR-8 | Page BRWM |
| FR-9 | Page Realty Income (O) |
| FR-10 | Page 3i Group (III) |
| FR-11 | Page CHDIV |
| FR-12 | Page QQQE |
| FR-13 | Page INFR + secteurs |
| FR-14 | Migration InveB modèle 3 couches |
| FR-15 | Cartes overview tous actifs + ETF actif |
| FR-16 | Navbar et routes lazy |
| FR-17 | Sync tierisée FMP 15 min / scrape 10 min / HYPE 10 min |
| FR-18 | Observabilité compteurs FMP/scrape |

**Total FRs : 18**

### Non-Functional Requirements (extraits PRD + epics)

| ID | Résumé |
|----|--------|
| NFR-1 | 1 appel provider max par actif par cycle sync |
| NFR-2 | AssetDaily uniquement en heures marché |
| NFR-3 | Pas d'appel provider sur GET HTTP |
| NFR-4 | FMP < 150/j, WARN > 200/j |
| NFR-5 | Scrape succès > 95 % heures SIX |
| NFR-6 | RAM VPS — cache par assetId |
| NFR-7 | ExternalCallExecutor retry/timeout |
| NFR-8 | User-Agent scrape identifiable |
| NFR-9 | Toasts erreur en français |
| NFR-10 | HYPE non régressé |
| NFR-11 | Pas de Flyway v1 |
| NFR-12 | Records + BigDecimal |
| NFR-13 | DTO front miroir back |
| NFR-14 | Ajout 9ᵉ actif < 2 h (config) |

**Total NFRs inventoriés : 14**

### Exigences additionnelles (architecture)

- Brownfield — pas de starter template greenfield
- `db-symbol` ≠ symbole provider (rétrocompat `INVE-B`)
- `ConfigurableAssetService` unique (ADR-01)
- Restart Docker pour reload YAML (ADR-07)
- Endpoint `GET /alerts/quarterly` (ADR-08)
- Gate spike VPS avant prod scrape (architecture §14)

### Évaluation complétude PRD

| Critère | Verdict |
|---------|---------|
| Scope MVP clair | ✅ |
| Non-goals explicites | ✅ |
| Success metrics | ✅ |
| Open questions | ⚠️ 3/4 résolues dans architecture ; ordre livraison OK |
| Spike validé CHDIV/QQQE | ✅ D-21 |
| Modèle 3 couches | ✅ Cohérent addendum + decision log |

**PRD : prêt pour implémentation.**

---

## 3. Epic Coverage Validation

### Matrice de couverture FR

| FR | Epic(s) | Story(s) | Statut |
|----|---------|----------|--------|
| FR-1 | 1 | 1.1, 1.2 | ✅ |
| FR-2 | 1 | 1.4, 1.5, 4.1 | ✅ |
| FR-3 | 1, 2 | 1.3, 2.4 | ✅ |
| FR-4 | 3 | 3.1, 3.3, 3.5, 4.2 | ✅ |
| FR-5 | 3 | 3.2, 3.4, 3.5, 4.2 | ✅ |
| FR-6 | 3 | 3.1, 3.2, 3.5 | ✅ |
| FR-7 | 5 | 5.1–5.3, 5.5 | ✅ |
| FR-8 | 6 | 6.1 | ✅ |
| FR-9 | 6 | 6.2 | ✅ |
| FR-10 | 6 | 6.3 | ✅ |
| FR-11 | 5 | 5.4 | ✅ |
| FR-12 | 5 | 5.4 | ✅ |
| FR-13 | 6 | 6.4 | ✅ |
| FR-14 | 4 | 4.1–4.5, 6.6 | ✅ |
| FR-15 | 4, 5, 6 | 4.6, 5.4, 6.1–6.5 | ✅ |
| FR-16 | 4, 5, 6 | 4.3, 4.4, 5.4, 6.1–6.6 | ⚠️ Voir gaps |
| FR-17 | 2, 5 | 2.2, 5.3 | ✅ |
| FR-18 | 2 | 2.3 | ✅ |

### FR manquants

**Aucun FR sans trace dans epics.**

### Couverture NFR dans stories

| NFR | Couvert explicitement | Gap |
|-----|----------------------|-----|
| NFR-1 | 1.2, 2.2, 5.2 | — |
| NFR-2 | 1.3, 2.1 | — |
| NFR-3 | 1.3, 1.5 | — |
| NFR-4 | 2.3 | — |
| NFR-5 | 2.3 (WARN fail rate) | 🟡 Pas de critère SM-5 explicite en prod |
| NFR-6 | 1.3 | — |
| NFR-7 | 1.2, 5.1 | — |
| NFR-8 | 5.1 | — |
| NFR-9 | 4.1 | — |
| NFR-10 | 2.2, 2.4, 4.5, 6.6 | — |
| NFR-11 | — | 🟡 Contrainte implicite, pas d'AC |
| NFR-12 | 3.3 | — |
| NFR-13 | 4.1 | — |
| NFR-14 | 1.1 | — |

### Statistiques

- **FR PRD :** 18
- **FR couverts epics :** 18 (100 %)
- **Stories :** 28
- **UX-DR dérivés :** 8/8 référencés dans epics

---

## 4. UX Alignment Assessment

### Statut document UX

**Non trouvé** — aucun `*ux*.md` dans `planning_artifacts/`.

### UI implicite

Oui — application web user-facing (dashboard.dokkcorp.ch), pages détail, overview, graphiques Chart.js, thème dark/copper.

### Alignement PRD ↔ Architecture ↔ Epics (UX)

| Exigence UX | PRD | Architecture | Epics | Aligné |
|-------------|-----|--------------|-------|--------|
| Thème dark/copper | Implicite UJ | project-context | UX-DR1, stories 4.2–4.3 | ✅ |
| PriceFreshnessBadge | FR-7 | ADR-14 | 4.3 | ✅ |
| QuarterlyAlertsBanner | FR-5, UJ-4 | ADR-08 | 4.6 | ✅ |
| AssetPage template | FR-16 | §9 | 4.3–4.4 | ✅ |
| Cartes génériques | FR-4, FR-5 | ADR-13 | 4.2 | ✅ |
| ETF section active | FR-15 | — | 5.4, 6.5 | ✅ |
| Badge scrape Yahoo | FR-7 | ADR-14 | UX-DR8, 5.4 | ✅ |
| AssetMainCard existant | Pattern InveB | Réutilisation | ⚠️ Non mentionné explicitement | 🟡 |

### Warnings UX

1. **Pas de spec UX formelle** — acceptable usage perso solo ; UX-DR dérivés dans epics compensent partiellement.
2. **FR-16 « Navbar »** — la navbar actuelle (`navbar.html`) ne liste que Home ; navigation réelle = cartes overview + routes. Story 6.6 dit « navbar à jour » sans préciser si liens navbar ou routes seules. **Ambiguïté produit.**
3. **Commodities « COMING SOON »** — reste en overview ; hors scope PRD mais peut confondre visuellement (dashboard.html L66–77).

---

## 5. Epic Quality Review

### Conformité structure epics

| Epic | Valeur utilisateur | Indépendance | Verdict |
|------|-------------------|--------------|---------|
| 1 Cadre backend | ✅ API unifiée pour Dokk | ✅ Standalone (inveb seul) | ✅ |
| 2 Sync & observabilité | ✅ Prix à jour automatiquement | ✅ Dépend Epic 1 | ✅ |
| 3 Div/fund/alertes | ✅ Rendement + alertes trimestrielles | ✅ Dépend 1.4 (DTO) | ✅ |
| 4 UI + InveB | ✅ InveB sans constantes | ✅ Dépend 1 + 3 | ✅ |
| 5 Scrape SIX | ✅ ETF SIX live | ✅ Dépend 1 + 2 | ✅ |
| 6 Portfolio élargi | ✅ Vue portefeuille complète | ✅ Dépend 1–4 | ✅ |

**Note :** Epics 1–2 titres légèrement techniques ; objectifs reformulés côté utilisateur — **acceptable** brownfield.

### Dépendances stories

| Check | Résultat |
|-------|----------|
| Forward dependencies intra-epic | ✅ Aucune détectée |
| Story 2.2 registre 5 actifs FMP avant Epic 6 | ✅ AC prévoit « inveb seul en phase initiale » |
| Epic 4 avant données div/fund | ✅ Ordre 1→2→3→4 documenté |
| Starter template Epic 1.1 | ✅ Correct brownfield (registre, pas clone) |

### Violations qualité

#### 🟠 Major (2)

**M-1 — Story 3.1 contradictoire avec ADR-07**  
AC : « mettre à jour sans redéployer du code »  
Architecture : restart Docker pour reload YAML.  
**Remédiation :** Corriger AC → « MAJ fichier YAML + restart backend Docker ».

**M-2 — FR-16 navbar ambigu**  
PRD exige navigation tous actifs ; navbar actuelle minimale ; story 6.6 vague.  
**Remédiation :** Clarifier dans story 6.6 : routes `app.routes.ts` obligatoires ; liens navbar optionnels ou liste déroulante — décision Dokk.

#### 🟡 Minor (4)

**m-1 — Epic 1–2 libellés techniques** — objectifs OK, cosmétique.

**m-2 — NFR-11 (pas Flyway)** — non référencé en AC ; rappel dans story 1.3 « entités existantes ».

**m-3 — AssetMainCard réutilisation** — story 4.3 devrait expliciter réutilisation composants `shared/` existants (PriceChart, DailyChart, AssetMainCard).

**m-4 — Story 3.5 placeholder YAML** — risque pages vides si placeholders non remplis ; acceptable si documenté.

#### 🔴 Critical

**Aucune violation bloquante.**

### Checklist best practices

- [x] Epics livrent valeur utilisateur (avec nuance Epic 1–2)
- [x] Pas de dépendance Epic N → N+1 inverse
- [x] Stories dimensionnées agent dev
- [x] Pas de forward dependencies
- [x] Pas de « create all tables » upfront
- [x] AC Given/When/Then
- [x] Traceabilité FR maintenue
- [x] Brownfield : migration InveB, pas greenfield setup

---

## 6. Architecture ↔ Epics Alignment

| Zone architecture | Epics | Aligné |
|-------------------|-------|--------|
| Packages `features/assets/` | Epic 1 | ✅ |
| `ConfigurableAssetService` | 1.3 | ✅ |
| `db-symbol` | 1.1 | ✅ |
| Crons tierisés | 2.2, 5.3 | ✅ |
| Yahoo parser | 5.1–5.2 | ✅ |
| `GET /alerts/quarterly` | 3.4 | ✅ |
| Plan migration M0–M5 | Ordre epics 1→6 | ✅ |
| Gate VPS scrape | 5.5 | ✅ |
| Suppression InveBService | 6.6 | ✅ |

### Écarts architecture / epics

| Écart | Sévérité | Note |
|-------|----------|------|
| Architecture liste 7 entrées registre dès M0 ; Epic 6 ajoute actifs FMP au registre | 🟡 | Cohérent si registre incrémental ; documenter dans 6.1–6.4 « ajouter entrée registre » |
| `GET /inveb` transition dual DTO | 🟡 | Story 4.5 laisse choix ouvert — trancher en implémentation |

---

## 7. Summary and Recommendations

### Overall Readiness Status

## ✅ READY WITH MINOR FIXES

Le triplet **PRD v2.1 + Architecture + Epics** est **suffisamment aligné pour démarrer l'implémentation** (Phase 4 / `bmad-dev-story`).  
Aucun FR manquant. Aucun blocant architectural. 2 corrections documentaires recommandées avant ou pendant Story 1.1.

### Issues par sévérité

| Sévérité | Count | Action |
|----------|-------|--------|
| 🔴 Critical | 0 | — |
| 🟠 Major | 2 | Corriger epics.md (3.1, 6.6) |
| 🟡 Minor | 4 | Optionnel pendant dev |
| ⚠️ UX doc absent | 1 | Acceptable hobby solo |

### Actions immédiates recommandées

1. **Corriger Story 3.1** — remplacer « sans redéployer du code » par « restart Docker backend » (ADR-07).
2. **Clarifier FR-16 dans Story 6.6** — routes lazy obligatoires ; préciser si la navbar doit lister les actifs ou si navigation via overview suffit (état actuel brownfield).
3. **Lancer dev** — `bmad-dev-story` sur **Story 1.1** (registre YAML + AssetRegistry).
4. **Avant Epic 5 prod** — exécuter gate **Story 5.5** (spike VPS Yahoo depuis Docker prod).
5. **Optionnel** — mini spec UX 1 page (composants + états badge) si @picasso a besoin de référence ; non bloquant.

### Ordre de démarrage validé

```
Story 1.1 → 1.2 → 1.3 → 1.4 → 1.5 → Epic 2 → Epic 3 → Epic 4 → Epic 5 (+ gate 5.5) → Epic 6
```

### Verdict gate dev

| Gate | Statut |
|------|--------|
| PRD final | ✅ |
| Architecture complete | ✅ |
| Epics & stories | ✅ |
| FR coverage 100 % | ✅ |
| Spike Yahoo (dev local) | ✅ |
| Spike VPS (prod) | ⏳ Avant deploy scrape |
| UX spec | ⚠️ Dérivée, non bloquant |

---

### Final Note

Cette évaluation a identifié **7 points d'attention** (2 major, 4 minor, 1 UX) sur **6 catégories** (documents, PRD, couverture, UX, qualité epics, alignement architecture). **Aucun n'empêche de commencer Story 1.1.** Les corrections M-1 et M-2 peuvent être intégrées en 5 minutes dans `epics.md` ou notées comme décisions d'implémentation.

---

_Rapport généré par workflow bmad-check-implementation-readiness — 2026-06-17_
