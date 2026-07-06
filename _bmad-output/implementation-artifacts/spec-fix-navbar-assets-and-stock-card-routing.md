---
title: 'Suppression des assets dans la navbar et correction du routage des cartes de stocks'
type: 'bugfix'
created: '2026-07-06'
status: 'done'
baseline_commit: '1ebe8e7fa16eb5577c3b24da2cc811861e4ba182'
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** La barre de navigation affiche les liens vers les différents assets (HYPE, INVE-B, etc.), ce que l'utilisateur souhaite supprimer. De plus, cliquer sur les cartes de stock du dashboard principal ne fonctionne pas car les liens redirigent vers `/:assetId` (ex: `/inveb`) au lieu de `/asset/:assetId` (ex: `/asset/inveb`), ce qui provoque une redirection vers la racine `/`.

**Approach:** Supprimer la ligne de navigation des assets de `navbar.html` et mettre à jour le composant `AssetDashboardCard` pour générer le bon chemin de routage pour les actions/stocks (ex: `/asset/inveb`), tout en conservant `/hype` pour Hype.

## Boundaries & Constraints

**Always:**
- Utiliser la syntaxe standalone d'Angular 21.
- Respecter le typage strict TypeScript sans utiliser `any`.

**Ask First:**
- (Aucun)

**Never:**
- Ne pas modifier d'autres parties du menu ou de la barre de navigation en dehors de la ligne d'assets.
- Ne pas réactiver Spring Security ou d'autres configurations backend.

</frozen-after-approval>

## Code Map

- [navbar.html](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/core/components/navbar/navbar.html) -- Fichier de template de la navbar à modifier pour supprimer la ligne d'assets.
- [navbar.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/core/components/navbar/navbar.ts) -- Composant navbar à nettoyer (supprimer les icônes devenues inutiles).
- [asset-dashboard-card.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/shared/components/asset-dashboard-card/asset-dashboard-card.ts) -- Composant carte de dashboard à modifier pour corriger la génération de lien pour les stocks.

## Tasks & Acceptance

**Execution:**
- [x] [navbar.html](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/core/components/navbar/navbar.html) -- Supprimer le bloc HTML correspondant à la barre de navigation des assets.
- [x] [navbar.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/core/components/navbar/navbar.ts) -- Retirer les imports et déclarations inutilisés de `Zap`, `TrendingUp`, `Landmark`.
- [x] [asset-dashboard-card.ts](file:///c:/Users/chju/Desktop/Dashboard/frontend/src/app/shared/components/asset-dashboard-card/asset-dashboard-card.ts) -- Modifier le computed `link` pour retourner `/hype` si l'asset est "hype", et `/asset/{id}` pour les autres assets.

**Acceptance Criteria:**
- Given le dashboard principal, when la page est affichée, then la barre de navigation ne contient plus les liens d'assets HYPE, INVE-B, etc.
- Given le dashboard principal, when on clique sur la carte d'action "Investor AB", then l'application navigue vers `/asset/inveb` et affiche la page de l'asset au lieu de rafraîchir le dashboard.
- Given le dashboard principal, when on clique sur la carte de crypto "Hype", then l'application navigue vers `/hype` et affiche la page correspondante.

## Verification

**Commands:**
- `npm run test` dans `frontend` -- expected: Tous les tests unitaires existants doivent passer avec succès.

**Manual checks (if no CLI):**
- Lancer le serveur local et vérifier visuellement l'absence de la sous-barre d'assets dans la navbar.
- Tester le clic sur les cartes Hype, Investor AB, BlackRock World Mining, Realty Income et vérifier la navigation.

## Suggested Review Order

**Routage et navigation**

- Modification de la logique de lien pour utiliser le préfixe /asset/ pour les actions.
  [`asset-dashboard-card.ts:20`](../../frontend/src/app/shared/components/asset-dashboard-card/asset-dashboard-card.ts#L20)

- Suppression de la ligne de navigation d'assets secondaire.
  [`navbar.html:31`](../../frontend/src/app/core/components/navbar/navbar.html#L31)

- Nettoyage des imports et propriétés Lucide inutilisés.
  [`navbar.ts:1`](../../frontend/src/app/core/components/navbar/navbar.ts#L1)

**Affichage des images**

- Ajout d'un slash initial pour charger correctement les logos depuis les sous-pages d'actifs.
  [`asset-main-card.html:21`](../../frontend/src/app/shared/components/asset-main-card/asset-main-card.html#L21)
