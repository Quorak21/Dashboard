---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - _bmad-output/_archive/2026-07-06-epic-6-complete/planning-artifacts/prds/prd-Dashboard-2026-06-17/prd.md
  - _bmad-output/_archive/2026-07-06-epic-6-complete/planning-artifacts/prds/prd-Dashboard-2026-06-17/addendum.md
  - docs/project-context.md
  - backlog.md
  - .agents/AGENTS.md
session_topic: 'Évolution produit du Dashboard personnel après Epic 6 — prochaine vague de valeur pour Dokk'
session_goals: 'Générer des pistes produit ancrées dans le PRD (JTBD, non-goals, contraintes perso/VPS/quota) ; distinguer quick wins vs nouvelles directions ; produire une shortlist actionnable pour un futur PRD v3'
selected_approach: 'ai-recommended + party-mode'
techniques_used:
  - Assumption Reversal
  - What If Scenarios
  - SCAMPER Method
  - Role Playing
ideas_generated: 70
context_file: 'PRD 2026-06-17 (substitut du product brief — aucun brief.md dans le repo)'
session_mode: 'party-mode (subagents indépendants)'
technique_execution_complete: true
session_active: false
workflow_completed: true
user_name: Dokk
date: '2026-09-14'
---

# Brainstorming Session Results

**Facilitator:** orchestrateur BMad (party-mode)
**Date:** 2026-09-14
**Mode:** Party-mode — 6 agents en subagents indépendants, 3 tours
**Participants:** 📊 Mary · 📋 John · 🎨 Sally · 🏗️ Winston · 💻 Amelia · 📚 Paige

## Session Overview

**Topic:** Évolution produit du Dashboard personnel après Epic 6 — prochaine vague de valeur pour Dokk

**Goals:**

- Générer des pistes produit au-delà du cadre multi-actifs déjà livré
- Distinguer quick wins (DEF/FF existants) vs nouvelles directions
- Respecter (ou consciemment revisiter) les non-goals du PRD : usage perso, pas d'auth, pas de trading, pas d'API fondamentaux payantes, simplicité
- Sortir une shortlist actionnable pour un futur mini-PRD / sprint

### Context Guidance

Aucun `brief.md` / `product-brief.md` n'existe dans le repo. La source stratégique utilisée comme **product brief de facto** est le PRD *Dashboard — Extension multi-actifs* (2026-06-17, v2.1), plus l'état réel du produit en septembre 2026.

**Vision PRD (brief) :** dashboard personnel Angular 21 + Spring Boot 4 pour suivre crypto + actions + ETF. Vue overview (cartes live) + pages détail. Ajouter un actif = config YAML, pas un nouveau service. Prix live (FMP / CoinGecko / Hyperliquid) ; dividendes et fondamentaux en YAML, alerte trimestrielle.

**Jobs-to-be-Done :**

1. Consulter en un coup d'œil la santé du portefeuille
2. Approfondir un actif (historique, dividendes, ratios)
3. Comparer des profils différents sans quitter l'UI dark/copper
4. Ajouter un futur actif par configuration
5. Être alerté quand les fondamentaux sont probablement obsolètes

**Non-goals explicites (PRD §5) :** auth / multi-user, alertes prix / push, export CSV, forex live, fondamentaux live via API payante, LLM in-app, app native, nouvelle crypto hors pattern HYPE.

**État actuel (sept. 2026) :** Epic 6 livré puis portefeuille **élagué**. Registry actuel : `inveb` (Investor AB, SEK) + `o` (Realty Income, USD). HYPE reste une page crypto riche dédiée (`/hype`). Route générique `/asset/:assetId` en place. Dernier commit : retrait de BRWM. Backlog DEF (factorisation RxJS, overview registry-driven, sync YAML ignoré, rate-limit FMP, lock pendant fetch…) + FF (tooltips fondamentaux, scrape SIX, reload YAML, Micrometer).

### Session Setup

Session **headless / party-mode** : Dokk a demandé `bmad-brainstorming` + `bmad-party-mode` basés sur le product brief. Pas de brief dédié → PRD + backlog + registry comme contexte. Approche techniques **AI-recommended**. Les agents BMad parlent en voix indépendantes (subagents) ; l'orchestrateur n'invente pas leurs propos.

## Technique Selection

**Approach:** AI-Recommended Techniques + Party Mode (Role Playing réel)

**Analysis Context:** Planning stratégique post-MVP, produit concret déjà en prod perso, utilisateur unique (Dokk, skill beginner), contraintes fortes (quota FMP, VPS, simplicité).

**Recommended Techniques:**

- **Assumption Reversal (deep) :** le PRD a beaucoup de non-goals figés en juin ; les revisiter maintenant que le cadre générique existe.
- **What If Scenarios (creative) :** et si le job n'était plus « ajouter des cartes » mais un autre verbe (décider, se rassurer, archiver).
- **SCAMPER (structured) :** améliorer le produit existant (HYPE + 2 actions) sans élargir le catalogue.
- **Role Playing (collaborative) :** exécuté via party-mode — chaque agent BMad est un subagent indépendant.

**AI Rationale:** Sujet complexe et transversal. Le risque LLM est de recycler les FF du backlog : inversion et what-if forcent des catégories orthogonales.

---

## Technique Execution Results

**Tour 1 — Diverge** (Mary, John, Sally, Winston) : Assumption Reversal + What If + Constraint Mapping.

**Tour 2 — Friction** (Amelia, Paige, John) : SCAMPER + Resource Constraints + Yes-And / désaccord.

**Tour 3 — Trancher** (Mary, Winston, Sally) : réponse à la mise en demeure de John (*si Dokk ouvre, ne fait rien, referme — c'est un succès ?*).

### Creative Facilitation Narrative

Le brief de juin mesurait encore SM-2 (« un 9ᵉ actif FMP en < 2 h »). Le git de septembre *retire* BRWM. Toute la table a traité cet élagage comme un **signal produit**, pas un trou à reboucher. Le désaccord utile n'était pas « faut-il plus d'actifs » (non) mais **quel verbe embauche le dashboard** : enseigner (Mary), fiabiliser l'infra (Winston R1), ou **tranquilliser en 90 secondes** (John + Sally). Le tour 3 a convergé : le regard est inerte ; la stratégie se commet *hors écran* (YAML + journal) ; la fiabilité a une forme (âge / échec / marché fermé) mais **pas** un théâtre de quota.

### Session Highlights

**Forces de la table :** inversion agressive des non-goals ; scènes UX concrètes ; veto d'Amelia sur les nouvelles couches ; budget de phrases de Paige.

**Breakthrough :** le live n'est pas on/off — c'est un **droit de tempo** (HYPE pulse, REIT loyer). SM-2 est un cadavre. La chaise vide BRWM à l'écran est du théâtre (commit git déjà passé, pas de tombe).

**Energy flow :** R1 consensus anti-expansion → R2 clash école / quota / densité → R3 contrat : lecture 90s = succès ; write YAML = exception éditoriale.

---

## Inventaire des idées (par tour)

Les idées sont numérotées pour la table d'organisation. Les formulations sont celles des agents (condensées au format session, voix intacte).

### Tour 1 — Mary (Assumption Reversal)

**[R1-M1] Concentration : Trois thèses, zéro remplissage**
_Concept_: L'élagage a voté. Vague « cockpit concentration » : thèse d'une phrase + kill-criterion par actif, depuis l'overview. JTBD 1 devient santé décisionnelle, pas santé prix.
_Novelty_: Traiter l'élagage comme signal, pas comme trou.

**[R1-M2] Rituel : le job n'est pas le coup d'œil du matin**
_Concept_: Surface « qu'est-ce qui a changé dans *ma* thèse depuis 7 jours » — prix, YAML, stale — sans push.
_Novelty_: Journal de portefeuille au rythme humain, pas au tick FMP.

**[R1-M3] Tension : comparer les régimes, pas les fiches**
_Concept_: Planche où HYPE / InveB / O se frottent (devise, yield vs croissance, liquidité). Le trou est *entre* les routes.
_Novelty_: Job transversal que le PRD nomme (JTBD 3) et que le drill-down dilue.

**[R1-M4] Silence : alerter l'absence, pas le trimestre**
_Concept_: Risque d'un portefeuille à 3 lignes = Dokk qui arrête de regarder. Signal dans l'overview, pas push.
_Novelty_: Alerter le désengagement, pas le prix.

**[R1-M5] École : le YAML est le cours, pas le tuyau**
_Concept_: Vague pédagogique pour Dokk-l'apprenant — relier registry → carte → détail, sans LLM in-app.
_Novelty_: Double job outil + terrain de stage, jamais écrit dans le PRD.

**[R1-M6] Lettre : export vers soi-même**
_Concept_: Capsule (thèses, kill, stale, 3 prix) pour Dokk-J+180. Preuve : BRWM retiré sans mémoire du pourquoi.
_Novelty_: Artifact de décision, pas CSV.

**[R1-M7] Bruit : rétrograder les prix live**
_Concept_: Overview par défaut = allocation / devise / yield YAML / fraîcheur ; live relégué au détail.
_Novelty_: Inverse exact du PRD 2026-06-17.

### Tour 1 — John (What If / JTBD)

**[R1-J1] Le feu vert du non-agir** — embauche pour *tranquilliser*. Trois pastilles + verdict « tiens / regarde ». Succès = temps *hors* produit.

**[R1-J2] La concentration comme job** — protéger un portefeuille étroit. Retirer = acte de première classe. SM-2 inversé.

**[R1-J3] Les deux minutes sont le produit** — overview = *la* surface, pas un hub.

**[R1-J4] Relire la thèse, pas le tick** — confronter live et conviction lente (YAML).

**[R1-J5] Les trois restants *sont* la comparaison** — arbitrer HYPE vs cashflow O vs holding InveB.

**[R1-J6] Le PRD a déjà gagné — hygiène de conviction** — ne pas reremplir, ne pas checker HYPE dix fois, ne pas oublier que les fondamentaux d'O pourrissent.

**[R1-J7] Fermer l'anxiété** — un état, une date de fraîcheur, un « tu peux partir ». Inverse des alertes prix.

**[R1-J8] Moins comme métrique de valeur** — rendre visible *pourquoi* un actif mérite sa carte.

*Why? laissés au tour 2 : (1) BRWM sorti pourquoi ? (2) comparaison crypto/action/ETF = job réel ou reliquat PRD ?*

### Tour 1 — Sally (scènes)

**[R1-S1] The Morning is the Product** — ne jamais tap ; détail = exception si carte clignote.

**[R1-S2] The Cut, Not the Chart** — anxiété « O a coupé le dividende ? » Oui/non, pas une leçon FFO. HYPE n'a pas cette carte.

**[R1-S3] Three Chairs, No Fourth** — desktop 3 colonnes ; mobile swipe ; le vide n'est pas un CTA « ajouter ».

**[R1-S4] One Number Per Breath** — un hero, un Δ24h, un verdict 3 mots. Couper, pas FF-01 partout. SM-C1.

**[R1-S5] Quiet Alarm on the Card** — stale = filet copper pointillés sur la carte. Pas de 4ᵉ carte grise « bientôt ».

**[R1-S6] Pocket Scan vs Desk Stare** — mobile 90s overview-only ; desktop plan large + hover. Deux ritournelles.

**[R1-S7] HYPE is a Pulse, REITs are a Lease** — deux cinématographies. Interdit de coller la carte loyer à HYPE.

### Tour 1 — Winston (contraintes)

**[R1-W1] Quota as a Feature** — compteur restant, prochain refresh, stale explicite. DEF-06/07/08 au service de la vérité live.

**[R1-W2] Stale is a First-Class State** — badges market closed / last good tick / fetch failed / locked. Sync *moins*, jamais mentir.

**[R1-W3] YAML is the Product** — FF-03 reload sans redeploy. Trois lignes = le produit. Quatre = un autre quota.

**[R1-W4] Let HYPE Leave the Template** — deux familles, deux clocks. Duplication assumée plutôt qu'AssetStrategy.

**[R1-W5] Three-Asset Overview, Not a Registry Demo** — layout figé 3 blocs. Recâbler à la main si BRWM revient.

**[R1-W6] FMP is a Pipe — don't exercise SIX scrape** — FF-02 parachute, pas roadmap.

**[R1-W7] The 90-Day Alarm is the App** — « saisi le YYYY-MM-DD » à l'écran. YAML reste source humaine.

**[R1-W8] Stop Generic Service Creep** — plateforme finie pour N=3. Cron, rate limit, lock, caches. Pas un 4ᵉ adapter.

### Tour 2 — Amelia (SCAMPER)

**Désaccord nommé :** Mary — ne pas rétrograder le *chiffre* live ; rétrograder le théâtre de fraîcheur. Why1 : BRWM mort hors app. Why2 : comparaison classes d'actifs = reliquat PRD.

**[R2-A1] No Fourth Chair** — freeze `assets-registry.yml`. AC = interdiction d'un 4ᵉ `id`.

**[R2-A2] Overview Is The Job** — `/` = les 90s ; `/asset/:assetId` = appendix.

**[R2-A3] Stale On Card, Not Ghost Price** — badge + last success ; HYPE pulse ; quota = chrome ops, pas 4ᵉ service.

**[R2-A4] Thesis Field In YAML** — clés `thesis` + `kill` sur inveb/o. Pas de CMS. Reload = FF-03.

**[R2-A5] Silence = Last Fetch Age** — `updatedAt` déjà dans l'API. Pas de moteur d'habitude.

**[R2-A6] Kill SM-2 And Generic Scrape** — HYPE hors template, route dédiée, point.

**[R2-A7] N=3 Lock As Feature** — tests figent N=3. Empty-slot = produit, pas placeholder.

**[R2-A8] YAML As School, Not CMS** — commentaires + 2 clés ; capsule J+180 = 1 bloc markdown *dans le YAML*. 0 UI settings.

### Tour 2 — Paige (langage)

**[R2-P1] Trois surfaces, trois verbes** — écran oriente · YAML décide · journal.md se souvient. Si la même phrase est aux 3 endroits = bruit.

**[R2-P2] Le dashboard n'est pas une école** — *désaccord Mary*. L'apprentissage se déporte hors écran.

**[R2-P3] Le silence n'est pas une hygiène** — *désaccord Sally*. Thèse + kill + hero : trois phrases ancrées, jamais en infobulle.

**[R2-P4] Capsule 140 caractères** — présent, datée. Si tu ne peux pas la dire en une ligne, tu n'as pas une thèse.

**[R2-P5] Retirer = phrase de première classe** — une ligne datée dans `journal.md`. Zéro nécrologie à l'écran. YAML : l'entrée meurt avec le commentaire.

**[R2-P6] Alerte 90j = date saisie, pas leçon** — « thèse périmée » + date. Tampon, pas cours.

**[R2-P7] Tuer FF-01 tooltips** — DEF-10 (titres EN) est le vrai bug langage. Franciser le visible. Refuser le wiki miniature.

**[R2-P8] Budget de phrases** — 1 à l'écran, 2 dans le YAML (`thesis`, `kill`), N dans le journal (N = actes).

### Tour 2 — John (cross-talk)

**ASSUMPTION BRWM :** thèse morte hors app. **ASSUMPTION comparaison :** reliquat PRD. Dokk ne compare pas HYPE à un REIT ; il respire trois tempos.

**Vague gagnante :** glance 90s. Séquence : film → fiabilité *invisible* → mémoire *après*. **Refuse** école YAML. **Refuse** quota on stage.

**[R2-J1] Trois horloges, un silence** — HYPE = pouls ; action = séance ; REIT = loyer. Live = droit de tempo, pas feature globale.

**[R2-J2] Chaise vide nominale** — tombeau 30 jours puis disparition. (Tué au tour 3.)

**[R2-J3] Mensonge daté, pas quota** — verbe *croire*. « Vrai à 07:14 » / « figé ». Jamais « 12/50 calls ».

**[R2-J4] YAML = testament, pas leçon** — le système se souvient *pourquoi* Dokk a embauché cette ligne.

**[R2-J5] Une alarme, pas un cours** — une interruption par visite. Irréversible gagne : cut dividende > tick HYPE > REIT inchangé.

**[R2-J6] Mobile = verdict. Desktop = scène du crime** — comparaison n'est pas un job d'ouverture.

**[R2-J7] HYPE n'est pas un ticker qui a réussi le casting** — unique raison pour laquelle le silence 90s peut être *faux*. Divergence de contrat, pas de skin.

*Mise en demeure : si ne rien faire est un succès, pourquoi Mary et Winston ont-ils encore besoin que Dokk fasse quelque chose ?*

### Tour 3 — Mary (réponse)

Open–close vide = succès **si la concentration a déjà été commise**. Cède l'école à l'écran. Ne cède pas la mémoire de concentration.

**[R3-M1] Deux temps, un seul geste** — regard inerte ; concentration hors visite, une fois.

**[R3-M2] Testament = condition de silence** — si `thesis`+`kill` n'existent pas, le 90s n'est qu'une watchlist.

**[R3-M3] Chaise vide 30j = théâtre** — le commit BRWM n'a pas laissé de tombe. Journal daté, puis disparition.

**[R3-M4] Ce qui reste de R1** — nommer le pari ; nommer la condition de mort ; dater la fin. Porter, pas SM-2.

**[R3-M5] L'alarme unique est le seul DO encore vivant** — si on tue aussi ça, il reste un film de prix.

### Tour 3 — Winston (recadrage)

Cède le quota UI (R1-W1 retiré du visible). Ne cède pas la forme produit de la fiabilité : **âge / fetch raté / marché fermé**.

**[R3-W1] Contrat d'âge, pas de quota** — tampon `as_of` ; quota = log only.

**[R3-W2] Trois horloges, une seule à l'œil** — marché / fetch / YAML ; UI = âge + fermé + échec. Ne pas collapser les trois gris.

**[R3-W3] Lock = YAML, pas mutex user-facing** — seul write humain = éditorial (`thesis`, `kill`).

**[R3-W4] Fetch raté = nombre d'hier assumé** — tick visible, marqué failed. Jamais hide.

**[R3-W5] HYPE = route + client, pas flag de template** — même registry et tampon d'âge ; fetch path séparé. Duplication > `if (hype)`.

**[R3-W6] Cron FF-03 = plumbing du rituel** — Dokk ne « lance » rien. Write YAML = exception si le récit a bougé.

*Seule action restante : éditer deux clés YAML quand le récit change — pas un healthcheck.*

### Tour 3 — Sally (scène shippée)

**[R3-S1] La fermeture verte** — succès = UI qui refuse de performer. Un nombre, une phrase, zéro alarme.

**[R3-S2] Loi mobile → desktop** — on ne « responsive » pas le drame : on le séquence.

**[R3-S3] Kill hors carte, in-scène** — thèse toujours là ; kill seulement s'il est l'alarme de la visite. Visibilité ≠ permanence.

**[R3-S4] Fetch mort** — nombre éteint, thèse grisée datée, filet cuivre cassé « on ne sait pas ». Retry une fois.

**[R3-S5] Marché fermé** — horloges avant le chiffre. HYPE pulse encore. Fermé ≠ cassé.

**[R3-S6] Thèse périmée — et pas de tombe** — phrase oxydée « cette phrase n'a plus cours ». BRWM déjà parti : pas de stèle. L'histoire est dans le journal.

---

## Idea Organization and Prioritization

### Thematic Organization

**Thème A — Le regard de 90 secondes est le produit**
_Focus: overview = film entier ; succès = refermer sans tap._
Idées : R1-J1, R1-J3, R1-J7, R1-S1, R1-S4, R2-A2, R2-J5, R3-S1, R3-S2.
_Pattern :_ JTBD 1 du PRD, mais le succès n'est plus « consulter » — c'est **tranquilliser**.

**Thème B — Trois horloges / droit de tempo**
_Focus: HYPE pulse, action séance, REIT loyer. Live n'est pas une religion._
Idées : R1-S7, R1-W4, R2-J1, R2-J7, R3-W2, R3-W5, R3-S5.
_Pattern :_ réconcilie Mary (démote le live générique) et Sally (HYPE doit pulser).

**Thème C — Fiabilité muette**
_Focus: âge / échec / marché fermé visibles. Quota, lock mutex, cron = plumbing._
Idées : R1-W2, R2-A3, R2-J3, R3-W1, R3-W4, R3-S4, R3-S5. *(R1-W1 quota-as-feature = rejeté au T3.)*
_Pattern :_ DEF-06/07/08 deviennent de la valeur utilisateur *seulement* via le tampon `as_of` et l'échec honnête.

**Thème D — Mémoire de concentration (hors école UI)**
_Focus: YAML testament (`thesis`, `kill`) ; journal pour les sorties ; 1 phrase à l'écran._
Idées : R1-M1, R1-M6, R1-J2, R1-J4, R1-J8, R2-A4, R2-P1–P8, R2-J4, R3-M1–M4, R3-S3, R3-S6.
_Pattern :_ Mary cède l'école à l'écran ; le DO stratégique est *antérieur* à la visite.

**Thème E — Freeze N=3 / anti-expansion**
_Focus: registry gelé ; SM-2 cadavre ; pas de scrape SIX ; pas de 4ᵉ chaise._
Idées : R1-J2, R1-J8, R1-S3, R1-W5, R1-W6, R1-W8, R2-A1, R2-A6, R2-A7, R3-M3, R3-S6.
_Pattern :_ l'architecture générique Epic 6 est *finie* pour ce portefeuille. Recâbler à la main si un 4ᵉ revient.

**Thème F — Une alarme irréversible**
_Focus: une interruption par visite ; cut dividende > tick HYPE ; kill n'habite pas la carte au quotidien._
Idées : R1-S2, R1-S5, R1-W7, R2-J5, R3-M5, R3-S3.
_Pattern :_ seul DO encore vivant *pendant* une visite.

### Breakthrough Concepts

- **SM-2 est un cadavre.** Mesurer l'expansion d'un portefeuille qu'on émonde.
- **Live = droit de tempo**, pas un on/off produit.
- **Trois surfaces de langage** (écran / YAML / journal) — budget de phrases.
- **90s sans clic = succès de lecture.** Write YAML = exception éditoriale.

### Idées tuées (explicitement)

| Idée | Tueur | Pourquoi |
|------|--------|----------|
| Quota as a feature (UI) | John T2 + Winston T3 | Pas un job. Voyant salle machine. |
| Dashboard-as-school à l'écran | John, Paige, Mary T3 (cède) | Dokk n'embauche pas un prof. |
| Chaise vide BRWM 30 jours | Mary T3, Sally T3 | Commit déjà passé ; fanfiction. |
| Rétrograder le *chiffre* live | Amelia | Hero sans prix = dashboard mort. On date, on ne cache pas. |
| 4ᵉ actif / scrape SIX / SM-2 | table entière | Élagage = signal. FF-02 = parachute. |
| FF-01 tooltips FFO | Paige, Sally | Glossaire sur le pare-brise. DEF-10 (FR) est le vrai bug langue. |
| CMS / AssetStrategy / LLM / bus | Amelia | Nouvelle couche = théâtre. |
| Comparaison crypto/action/ETF comme job d'ouverture | John ASSUMPTION | Reliquat PRD. Desktop = autopsie *après* le doute. |

### Prioritization Results

Critères : impact sur le job 90s · faisabilité N=3 sans nouvelle couche · alignement simplicité / quota / VPS · originalité vs FF backlog.

**Top 3 high-impact (vague produit v3)**

1. **Overview = le film** (Thème A) — un hero, une thèse, état all-green conçu, détail en appendix.
2. **Contrat d'âge** (Thème C) — `as_of` / fetch failed / marché fermé sur la carte. Plumbing DEF-06/07/08 *invisible*.
3. **Testament YAML** (Thème D) — `thesis` + `kill` ; phrase à l'écran ; kill seulement comme alarme ; sorties dans `journal.md`.

**Quick wins (cette semaine, sans PRD)**

- Une ligne `journal.md` pour BRWM (pourquoi sorti) — 0 UI.
- DEF-10 : titres de charts en français.
- Freeze mental : pas de nouvel `id` dans `assets-registry.yml`.

**Breakthrough / plus tard**

- Trois horloges (HYPE pulse vs REIT loyer) comme *loi* de tempo, pas juste un skin.
- Loi de séquence mobile verdict → desktop autopsie.
- FF-03 reload YAML sans redeploy — plumbing du rituel, pas une UI settings.

**Hors vague (ne pas faire)**

- FF-01 tooltips fondamentaux.
- FF-02 scrape SIX.
- Jauge quota / last cron à l'écran.
- Tombe BRWM.
- 9ᵉ actif « parce que le registry le permet ».

---

## Action Planning

### P0 — Mini-brief v3 (cette semaine)

**Why:** Capturer le contrat avant qu'un sprint recycle SM-2.

1. Rédiger 1 page : job = tranquilliser en 90s ; N=3 gelé ; live = droit de tempo ; fiabilité = âge pas quota ; YAML = testament.
2. Y coller les non-goals *nouveaux* : école UI, quota UI, chaise vide, tooltips FFO, scrape SIX.
3. Enchaîner `bmad-prd` *seulement* si Dokk valide ce brief.

**Success :** Dokk peut dire non à une carte/ticker en citant le brief.

### P1 — Overview 90s + stale honnête

**Why:** C'est le film. Fichiers déjà là : `frontend/.../dashboard`, `asset-page`, route `/`.

1. Un chiffre hero + Δ24h par carte ; FFO/NAV derrière un tap volontaire (ou absents du glance).
2. Tampon `as_of` / marché fermé / fetch failed **sur la carte** (pas toast-only).
3. État all-green : cuivre mat, zéro alarme, nulle part où tap — conçu, testé.
4. Back : DEF-06 interval YAML, DEF-07 throttle FMP, DEF-08 fetch hors lock — *sans* les exposer.

**Success :** Dokk referme en < 90s les matins calmes ; un fetch mort ne ressemble pas à un marché fermé.

### P2 — Testament YAML (après P1)

**Why:** Condition de silence de Mary. Amelia : 2 clés, 0 CMS.

1. `thesis` + `kill` (et date de serment) dans le YAML inveb/o.
2. Une ligne ≤ 140 car. sous le hero. Kill **pas** en permanence : seulement si c'est l'alarme de la visite.
3. Sortie d'actif = une ligne datée dans `journal.md`, pas d'UI.
4. Thèse périmée (90j) = phrase oxydée + date, pas un cours FFO.

**Success :** un visiteur du soir lit *pourquoi* O est encore là, sans infobulle.

### P3 — Contrats HYPE vs stocks (ne pas unifier)

**Why:** Winston + John + Sally. Route `/hype` déjà dédiée.

1. Interdire de faire rentrer HYPE dans le template `/asset/:assetId`.
2. Pulse HYPE vs loyer InveB/O = deux cinématographies, même skin dark/copper.
3. Duplication assumée plutôt qu'un `if (hype)` dans la carte générique.

**Success :** HYPE qui ne pulse pas est un bug ; O qui pulse au tick est un bug.

---

## Session Summary and Insights

**Key Achievements:**

- ~70 idées, 3 tours, 6 voix indépendantes, ancrées dans le PRD (brief de facto) et l'état git (BRWM retiré, registry N=2 FMP + HYPE).
- Consensus anti-expansion. Désaccord utile tranché : 90s glance gagne ; école et quota UI perdus ; fiabilité garde une forme (âge).
- Shortlist P0–P3 actionnable sans nouvelle abstraction.

**Session Reflections:**

- Le « product brief » demandé n'existait pas en fichier : travailler à partir du PRD archivé + du registry réel a *révélé* le drift (SM-2 vs élagage). C'est le vrai input de la session.
- Party-mode a empêché la convergence molle : Amelia a tué le hide-price ; Paige a tenu 3 phrases contre le zéro-mots ; Mary a cédé l'école sans céder la stratégie.
- Prochaine skill naturelle si Dokk valide : `bmad-product-brief` (créer enfin un `brief.md` v3) puis éventuellement `bmad-prd`.

**What Makes This Session Valuable:**

- Le verbe a changé : de *ajouter* à *tranquilliser*.
- Les DEF/FF du backlog sont recadrés : hygiène vs vague produit.
- Un budget de langage (écran / YAML / journal) que le PRD 2026-06-17 n'avait pas.

**Next steps for Dokk:**

1. Relire ce document — surtout les idées tuées, pas seulement les top 3.
2. Décider P0 : brief v3 oui/non.
3. Si oui : ne pas ouvrir un 4ᵉ ticker « en attendant ».
4. Quick win immédiat : ligne journal BRWM + freeze registry.
