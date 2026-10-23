# CLAUDE.md — AntiGaspi

Application anti-gaspillage : inventaire d'aliments triés par urgence de péremption et recettes générées par un LLM. Voir le [README](README.md) pour le cadrage complet.

## Stack
- Backend : Java 21, Spring Boot 4, Spring AI 2, Maven multi-module (`pom.xml` agrégateur à la racine, module `backend/`).
- Frontend : Angular 22 (composants autonomes, signaux, `httpResource`), dans `frontend/`. Design issu de Claude Design (projet « Maquettes UI formulaire », design system Emocio) : tokens dans `frontend/src/styles/tokens.css`, police Quicksand (Fontsource), icônes `@lucide/angular`, rendu Markdown du chat avec `marked` (version exacte, dépendance de sécurité). Tests Vitest via `ng test`.
- LLM : Spring AI `ChatClient`, isolé derrière l'interface `RecipeGenerator`. Nova Lite via le proxy compatible OpenAI de la formation (starter Spring AI OpenAI ; variables `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL=nova-lite`).
- MCP : Spring AI MCP Server (starter webmvc, Streamable HTTP), actif uniquement avec le profil `mcp`. Tools exposés : `listerIngredients` (lecture) et `ajouterIngredients`, `modifierIngredients`, `supprimerIngredients` (écriture dans la base, un ou plusieurs aliments par appel).
- Données : inventaire et ustensiles en base PostgreSQL (docker compose) via Spring Data JPA, derrière des interfaces `*Repository`. Schéma par Flyway (`backend/src/main/resources/db/migration/`), amorçage unique par la migration Java `config/SeedDataMigration` (version 2) à partir de `backend/src/main/resources/data/*.json` (jours relatifs, `Clock`). Tests sur H2 en mémoire en mode PostgreSQL (`backend/src/test/resources/config/application.yml`, une base neuve par contexte Spring).
- Profil utilisateur (préférences alimentaires) : fichier Markdown `data/profil.md` (gitignoré), gabarit `data/profil.example.md`.
- Recette : format JSON standardisé (`title`, `servings`, `preparationMinutes`, `utensils`, `ingredients`, `steps`), décrit dans `docs/format-recette.md`.

## Architecture du backend
Découpage **par fonctionnalité**, puis par couche à l'intérieur de chaque fonctionnalité :

```
fr.antigaspi
├── ingredient/   model/ (Ingredient, IngredientDraft, Category, Unit, Urgency, IngredientView) · repository/ · service/ (IngredientService : lecture triée par urgence, ajout, modification, quantité, retrait, retrait groupé atomique ; IngredientNotFoundException → 404) · api/ (IngredientController : GET /api/ingredients et /api/ingredients/categories, POST, PUT /{id}, DELETE /{id}, DELETE ?ids=1,3 ; IngredientExceptionHandler : 404 avec `{"message"}`) · tool/ (IngredientTools : tool LLM listerIngredients · IngredientEditTools : tools du chat ajouterAliment, modifierQuantite, retirerAliments · IngredientMcpTools : tools MCP ajouterIngredients, modifierIngredients, supprimerIngredients (un ou plusieurs aliments, atomiques, via IngredientService.addAll/updateAll/removeAll ; sans `ChatTools`, donc absents du chat) · IngredientMcpConfig : expose au serveur MCP listerIngredients et ces trois tools, profil `mcp`)
├── utensil/      model/ (Utensil) · repository/ · service/ (UtensilNotFoundException → 404) · api/ (UtensilController : GET, POST, DELETE /api/utensils) · tool/ (UtensilTools : tools LLM listUtensils, addUtensil, removeUtensil)
├── profile/      model/ (UserProfile) · repository/ (Markdown : lecture et réécriture de la seule section Préférences) · service/ · api/ (ProfileController : GET et PUT /api/profile) · tool/ (ProfileTools : tool LLM consulterPreferences)
├── recipe/       model/ (Recipe, RecipeIngredient, RecipeRequest, RecipeContext, GeneratedRecipe, RecipeIssue, DishType, CookedRecipe, Consumption, Rating) · repository/ (CookedRecipeRepository : recettes cuisinées, recette en JSON dans la table `cooked_recipe`) · service/ (RecipeService, RecipeChecker : garde-fou post-LLM, CookingService : « J'ai cuisiné », historique et modification de l'avis, décrément de l'inventaire via `IngredientService.consume` et enregistrement dans la même transaction) · api/ (CookedRecipeController : POST /api/cooked-recipes (201), GET /api/cooked-recipes (plus récentes d'abord), PUT /api/cooked-recipes/{id}/feedback ; CookedRecipeExceptionHandler : 404 ; RecipeController : POST /api/recipes, une recette et un appel LLM par requête, `variant` facultatif de 0 à 2 : RecipeService impose alors un type de plat (`DishType`) et, si possible, un aliment principal différents ; RecipeExceptionHandler : 502 si le LLM échoue) · llm/ (RecipeGenerator, Spring AI, prompts ; le prompt utilisateur reprend les `feedback-count` dernières recettes cuisinées avec leur avis) · tool/ (RecipeTools : tool LLM genererRecette, génération + garde-fou en un appel) · cli/ (démo terminal)
├── chat/         llm/ (ChatAssistant : ChatClient + mémoire + tous les beans ChatTools, ToolCalledEvent publié à chaque appel de tool) · model/ (ChatReply, ChatEntry, ChatRole) · api/ (ChatController : POST, GET et DELETE /api/chat, ChatAssistant.replyWithTools renvoie aussi les tools appelés et les recettes générées pour ce message (`recipes`, de la forme `{ recipe, issues }`, collectées via le `RecipeGeneratedEvent` que publie RecipeTools) ; ChatExceptionHandler : 502 si le LLM échoue) · cli/ (démo chat en terminal)
├── common/       utilitaires partagés (Resources : lecture JSON et texte ; ChatTools : marqueur des classes de tools exposées au chat ; api/ : ApiError et ApiExceptionHandler, 400 `{"message"}` commun à toutes les API REST)
└── config/       configuration Spring (Clock) et SeedDataMigration (amorçage des données, migration Flyway Java)
```

- `model/` : records et enums métier, sans dépendance à Spring.
- `repository/` : interface `XxxRepository` + implémentation (`JpaXxxRepository` avec son entité `XxxEntity` et son interface Spring Data `XxxJpaRepository`, package-private ; `MarkdownProfileRepository`). Les records de `model/` ne sont jamais des entités ; les quantités relues sont normalisées (`2.500` → `2.5`).
- `service/` : logique métier, seul point d'entrée pour les futurs `api/` (REST) et MCP.
- `llm/` : tout ce qui dépend du fournisseur de LLM, derrière `RecipeGenerator`.
- `tool/` : tools Spring AI (`@Tool`) d'une fonctionnalité, appelables par le LLM ; ils délèguent au service, sans logique métier. La classe implémente `ChatTools` pour être exposée au chat automatiquement.
- `api/` : contrôleurs REST et DTO (`XxxResponse`) d'une fonctionnalité, destinés au front ; ils délèguent au service, sans logique métier. Les valeurs refusées (`IllegalArgumentException`), paramètres mal typés et corps illisibles deviennent un 400 `{"message"}` via `common/api` ; une erreur plus précise (ex. 404) a son propre `@RestControllerAdvice` limité au contrôleur, avec `@Order(HIGHEST_PRECEDENCE)`.
- Une nouvelle fonctionnalité suit le même schéma. Les tests reprennent le package de la classe testée.

## Architecture du frontend
- `frontend/src/app/<fonctionnalité>/` : un dossier par écran (ex. `inventory/` : `InventoryPage` (ajout, édition au clic sur une ligne, mode sélection avec « Retirer (n) » et bandeau d'annulation), `IngredientForm` (ajout en rafale qui garde catégorie et unité, ou édition ; date laissée vide si elle est estimée), `InventoryStore` (lecture via `httpResource`, écritures via `HttpClient` puis rechargement, suppression différée de 5 s annulable : les aliments sont masqués tout de suite, le `DELETE` part à l'expiration), modèle (`UNITS` : codes et symboles), fonctions pures dans `urgency.ts` ; `profile/` : `ProfilePage`, `ProfileStore` avec modifications via `HttpClient`, sans mise à jour optimiste ; `recipes/` : `RecipesPage`, `RecipeDetailPage` (lien « J’ai cuisiné cette recette »), `CookPage` (`/recettes/:id/cuisine` : quantités utilisées préremplies dans l'unité de l'inventaire grâce à `unit-conversion.ts`, avis 👍/👎 et commentaire facultatifs, puis rechargement de l'inventaire), `RecipeStore` qui lance 3 appels `POST /api/recipes` en parallèle, chacun avec sa variante (index de la carte), et ignore les réponses d'une génération remplacée). `history/` : `HistoryPage` (recettes cuisinées dépliables, avis et commentaire modifiables) et `HistoryStore` (`httpResource` + PUT puis rechargement). `chat/` : `ChatPanel` (affiché à côté des recettes, onglets sous 1440 px), `ChatStore` (historique chargé une fois, une seule conversation en mémoire côté serveur ; recharge l'inventaire quand une réponse a appelé un tool d'écriture ; une recette générée s'affiche en carte « Voir la recette » qui l'ajoute à `RecipeStore.addFromChat` (index ≥ 100, cartes de l'écran Recettes intactes) puis ouvre `/recettes/<index>`) et `MarkdownPipe` (`markdown.ts` : réponses de l'assistant converties par `marked`, messages de l'utilisateur laissés en texte brut ; HTML brut du LLM échappé, images remplacées par leur texte alternatif, liens dans un nouvel onglet ; insérées via `[innerHTML]` pour passer par le sanitizer d'Angular, jamais de `bypassSecurityTrustHtml` ; styles globaux `.markdown` dans `styles.css`). `api-error.ts` : message des erreurs `{"message"}` de l'API. Spec `xxx.spec.ts` à côté du fichier testé.
- Navigation : `app.html` (4 entrées : Inventaire, Recettes, Historique, Profil ; barre latérale sur ordinateur, barre d'onglets en bas sur mobile, la même `nav` repositionnée par CSS) ; une route paresseuse par écran dans `app.routes.ts`.
- Les données viennent de l'API Spring (`/api/...`) ; en développement, `proxy.conf.json` redirige `/api` vers `127.0.0.1:8080`. Un store `providedIn: 'root'` (`httpResource`) est partagé entre les écrans qui en ont besoin (ex. pastille d'urgence de la navigation).
- Écrans, dans l'ordre : Inventaire (fait : ajout en rafale, édition, sélection multiple et retrait annulable), Profil (fait : préférences et équipement en liste avec retrait, sans interrupteur ; les retours sont dans l'écran Historique), Recettes (fait : réglages, 3 cartes, détail ; recettes non conservées après rechargement, « J'ai cuisiné » fait, pas de « À acheter »), Historique (fait : recettes cuisinées, avis et commentaire modifiables, repris dans le prompt), Chat avec le chef (fait : réponses rendues en Markdown, tools affichés après la réponse, pas de flux en direct).
- Les écrans de génération appellent le vrai LLM : aucun test ne le fait, la vérification en réel se fait en lançant backend et front (`npm start`).

## Commandes
- Base de données, nécessaire à tout lancement local (`spring-boot:run`, profils `demo`, `chat`, `mcp`) : `docker compose up -d db` (PostgreSQL 17 sur `127.0.0.1:5432`, volume `pgdata` ; `docker compose down -v` remet la base à zéro et relance l'amorçage). Les tests n'en ont pas besoin (H2).
- Tous les tests (sans appel LLM) : `mvn -q verify`
- Une classe de test : `mvn -q test -pl backend -Dtest=Classe#methode`
- Formatage : `mvn formatter:format` (le hook pre-commit lance `formatter:validate`)
- Démo en terminal (aliments urgents + 10 recettes JSON + bilan des échecs, appels réels au LLM) : `mvn -pl backend spring-boot:run -Dspring-boot.run.profiles=demo`
- Démo chat en terminal (le LLM appelle les tools inventaire, ustensiles, préférences et génération de recette au besoin, `quitter` pour sortir) : `mvn -pl backend spring-boot:run -Dspring-boot.run.profiles=chat`
- Serveur MCP Streamable HTTP (tools d'inventaire : lecture et écriture, sans authentification, hors conteneur ; base lancée avant par `docker compose up -d db`) : `mvn -pl backend spring-boot:run -Dspring-boot.run.profiles=mcp`, puis `claude mcp add --transport http antigaspi http://127.0.0.1:8080/mcp`.
  - Il écoute sur `127.0.0.1`, port 8080 par défaut. Ne pas combiner `mcp` avec `demo` ou `chat` : ces profils coupent le serveur web (`web-application-type: none`).
  - Le serveur MCP est désactivé par défaut (`spring.ai.mcp.server.enabled: false` dans `application.yml`) ; `application-mcp.yml` le réactive et fixe `protocol`, `name`, `version` et `server.address`.
- Frontend (depuis `frontend/`, Node 24) :
  - Installation : `npm ci`. Tests (sans réseau) : `npm test -- --no-watch`. Build : `npm run build`.
  - Lancement : démarrer le backend (`mvn -pl backend spring-boot:run`, port 8080), puis `npm start` ; l'appli est sur `http://localhost:4200` (pas `127.0.0.1` : le serveur écoute en IPv6).
- Tunnel Cloudflare du serveur MCP pour ChatGPT (backend lancé avec le profil `mcp`) : `cloudflared tunnel run antigaspi-mcp`, avec une configuration copiée de `deploy/cloudflared/config.example.yml` ; procédure dans `docs/mcp-tunnel-cloudflare.md`.
  - Le tunnel ne doit transmettre que `/mcp` (règle `path` du gabarit) : le backend sert aussi `/api/*`, qui modifie le profil et consomme le LLM. Le serveur MCP reste sans authentification et peut modifier l'inventaire (pas seulement le lire) : arrêter le tunnel après usage.
- Appli complète avec Docker : `docker compose up -d --build` (base, backend, front nginx sur `http://localhost:8081` ; le build du backend lance les tests). Démos : `SPRING_PROFILES_ACTIVE=demo docker compose run --rm backend` (ou `chat`). Le `.env` (facultatif, gabarit `.env.example`) n'est jamais copié dans l'image (`.dockerignore`) ; `./data` est monté en écriture pour `profil.md`.

## Conventions
- Code, commentaires, doc et messages de commit en français.
- Classe de test `XxxTest` pour la classe `Xxx`, même package, sous `src/test/java` : les hooks retrouvent les tests par ce nom.
- Tests déterministes : pas de réseau, heure figée via un `Clock` injecté, LLM simulé (faux `ChatModel`). Aucun test n'appelle le vrai LLM : la vérification en réel se fait avec la démo.
- Pas de test sans valeur : un test qui ne fait que vérifier une délégation, ou qui n'est jamais exécuté, n'est pas gardé.
- Frontend : même règles de test que le backend (déterministe, `HttpTestingController`, aucun appel réel) ; texte de l'interface en français.
- Branches `feat/<nom>` et `fix/<nom>`, PR vers `main`.

## Restrictions
- Ne jamais lire ni modifier `.env` (clés API), par aucun moyen. Seul `.env.example` est lisible.
- Ne jamais committer `.env`, une clé API ou `data/profil.md`, ni `cert.pem` ou `<UUID>.json` du tunnel Cloudflare.

## Méthode : TDD obligatoire
Toute feature ou correction suit le cycle Red → Green → Refactor :
1. **Red** : écrire d'abord les tests du plan de test validé. Si nécessaire, créer des stubs minimaux (signatures, `throw new UnsupportedOperationException()`) pour que le code compile, sans aucune logique. Lancer les tests et **constater qu'ils échouent** pour la bonne raison.
2. **Green** : écrire le code de production minimal qui fait passer les tests.
3. **Refactor** : nettoyer en gardant les tests verts.

Règles :
- Les tests sont la spécification : interdit de modifier, affaiblir ou désactiver un test pour le faire passer sans accord humain explicite. Un test qui semble faux est remonté à l'utilisateur.
- On ne commite qu'au vert, tests et code ensemble (le hook pre-commit lance les tests ciblés).
- En phase Red, le hook Stop (`stop-tests`) bloque une fois la fin du tour parce que les tests des stubs échouent : c'est attendu, il ne faut pas le contourner.
