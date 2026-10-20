# Socle backend, inventaire statique et génération de recette (20/10/2026)

Branche : `feat/socle-inventaire-recette`. Périmètre : **backend uniquement**, au niveau service. Pas d'API REST ni de base de données pour l'instant.

## Objectifs de la journée

- [x] Initialiser le projet (Spring Initializr).
- [x] Créer l'objet aliment (`Ingredient`) avec les informations nécessaires.
- [x] Avoir une liste **statique** d'aliments pour tester.
- [x] Lister ces aliments (« get all »), **triés par urgence de péremption**.
- [x] Déclarer les ustensiles disponibles de l'utilisateur. D'abord dans le Markdown, puis **en base** (JSON statique) après discussion, voir la [mise à jour](#mise-à-jour-après-discussion--ustensiles-en-base-et-format-de-recette).
- [x] Générer une recette à partir d'une liste d'aliments via un **LLM** (Bedrock Nova Lite).
- [x] Paramétrer le LLM : prompt système, modèle, température, etc.
- [x] Rendre le **TDD obligatoire** dans la config Claude Code.

## Décisions prises

| Sujet | Décision | Pourquoi |
|---|---|---|
| Méthode | **TDD** : tests écrits et vus en échec avant le code | Figer le comportement attendu avant d'implémenter |
| Versions | **Spring Boot 4.1.1**, Spring AI 2.0.1, Java 21 | Spring Initializr ne propose plus Spring Boot 3 ; le README parlait de « versions à confirmer » |
| API REST | **Pas encore** : on s'arrête aux services | Le front et le MCP viendront plus tard ; les contrôleurs seront de simples façades |
| Données de test | Fichier JSON avec des **jours relatifs** (`"expiresInDays": 2`) | Des dates en dur seraient toutes périmées dans une semaine |
| Ustensiles | ~~Section `## Équipement` du profil~~ → **en base** (`utensils.json` pour l'instant) | Décidé après discussion, voir la mise à jour ci-dessous |
| Format de recette | **JSON standardisé** ([docs/format-recette.md](../format-recette.md)) | Pouvoir stocker les recettes en base plus tard |
| LLM | Bedrock Nova Lite via Spring AI, isolé derrière `RecipeGenerator` | Pour changer de fournisseur, on ne touche qu'à la configuration |
| Date manquante | Estimée à date d'ajout + durée **minimale** de la catégorie | Mieux vaut estimer trop court que trop long |

## Ce qui a été réalisé

### 1. Config Claude Code : TDD obligatoire (commit `chore(claude)`)

- Nouveau fichier [CLAUDE.md](../../CLAUDE.md), qui manquait alors que tous les agents y font référence : stack, commandes, conventions, restrictions et la section **« Méthode : TDD obligatoire »**.
- [feature-orchestrator](../../.claude/skills/feature-orchestrator/SKILL.md) : l'étape 3 est découpée en **3a, les tests d'abord (Red)**, puis **3b, l'implémentation (Green)**.
- Agent [implementer](../../.claude/agents/implementer.md) : il reçoit des tests qui échouent déjà et doit les faire passer sans les modifier.
- Skill [generate-tests](../../.claude/skills/generate-tests/SKILL.md) : on l'utilise *avant* le code. Les stubs de compilation sont autorisés, et la phase Red est vérifiée.
- Agent [test-plan-writer](../../.claude/agents/test-plan-writer.md) : son plan de test devient la spécification.

> ⚠️ En phase Red, le hook `stop-tests` bloque une fois la fin du tour de Claude parce que les tests échouent. C'est normal, il ne faut pas le contourner.

### 2. Socle (commit `chore: initialiser le backend`)

- [pom.xml](../../pom.xml) agrégateur à la racine, avec le module `backend`. Sa présence active les hooks Maven de `.claude/`.
- [backend/pom.xml](../../backend/pom.xml) contient :
  - les starters Web, Validation et Spring AI Bedrock Converse ;
  - `formatter-maven-plugin`, que le hook pre-commit utilise pour lancer `formatter:validate` ;
  - le profil Maven `llm` pour les tests qui appellent un vrai LLM.
- Le wrapper Maven (`mvnw`) est à la racine.

### 3. Inventaire (`fr.antigaspi.ingredient`)

| Fichier | Rôle |
|---|---|
| `Ingredient` | L'aliment : id, nom, catégorie, quantité, unité, péremption (optionnelle), date d'ajout |
| `Category` | Les 11 catégories du README, avec une durée de conservation min/max (**valeurs provisoires**) |
| `Unit` | `g`, `kg`, `ml`, `l`, `pièce` |
| `Urgency` | 🔴 périmé ou à consommer aujourd'hui, 🟠 ≤ 3 j, 🟡 ≤ 7 j, 🟢 au-delà (seuils en constantes) |
| `IngredientView` | L'aliment plus la date effective, les jours restants, l'urgence et l'indicateur `estimated` |
| `IngredientRepository` / `JsonIngredientRepository` | Lecture de [ingredients.json](../../backend/src/main/resources/data/ingredients.json) (14 aliments). Sera remplacé par une implémentation JPA |
| `IngredientService` | `listByUrgency()` pour le get all trié, `findMostUrgent(n)`, `findByIds(ids)` |

**Tri** : date effective croissante. À date égale, une date réelle passe avant une date estimée, puis on trie par nom.

La date du jour vient d'un `Clock` injecté (`ClockConfig`) : les tests figent ainsi « aujourd'hui ».

### 4. Profil (`fr.antigaspi.profile`) et ustensiles (`fr.antigaspi.utensil`)

- Gabarit versionné : [data/profil.example.md](../../data/profil.example.md). Chacun le copie en `data/profil.md`, qui est gitignoré.
- `ProfileService.getProfile()` s'appuie sur `MarkdownProfileRepository`, qui lit les lignes `- …` de la section `## Préférences`. Les titres sont reconnus sans tenir compte des accents ni de la casse.
- Le fichier est relu à chaque appel : une modification à la main est prise en compte sans redémarrer.
- Si `data/profil.md` est absent, le gabarit sert de repli. S'il n'y a ni l'un ni l'autre, le profil est vide.
- Les ustensiles disponibles sont lus dans [utensils.json](../../backend/src/main/resources/data/utensils.json) par `UtensilService.listAll()`. Ils seront stockés en base plus tard (voir la mise à jour plus bas).

### 5. Génération de recette (`fr.antigaspi.recipe`)

```
RecipeService.generate(RecipeRequest)
  ├─ aliments : les N plus urgents (défaut 8) ou ceux choisis par id
  ├─ ustensiles : UtensilService (utensils.json, en base plus tard)
  ├─ profil   : ProfileService (préférences)
  └─ RecipeGenerator ──► SpringAiRecipeGenerator ──► Bedrock (Nova Lite)
                              prompt système : resources/prompts/recipe-system.md
                              prompt utilisateur : RecipePromptBuilder
                              réponse JSON ──► record Recipe (sortie structurée Spring AI)
```

- `Recipe` suit le [format JSON standardisé](../format-recette.md) : `title`, `servings`, `preparationMinutes`, `utensils`, `ingredients` (nom, quantité, unité, `ingredientId` optionnel) et `steps`.
- `RecipeRequest(ingredientIds, servings, maxMinutes)` : tous les champs sont optionnels. `RecipeRequest.mostUrgent()` utilise les valeurs par défaut.
- Les erreurs du LLM (appel ou JSON invalide) sont transformées en `RecipeGenerationException`. Une requête invalide (aucun aliment, portions ≤ 0) lève une `IllegalArgumentException` **sans appeler le LLM**.
- La durée de l'appel et les tokens consommés sont journalisés, pour suivre le coût Bedrock.

### 6. Paramétrage du LLM

- **Prompt système** : [recipe-system.md](../../backend/src/main/resources/prompts/recipe-system.md). Il contient :
  - le rôle de chef anti-gaspi ;
  - la priorité aux aliments urgents et les règles de sécurité alimentaire (jamais de viande ou de poisson périmé) ;
  - l'obligation d'utiliser **uniquement les ustensiles disponibles** et de respecter les préférences ;
  - la description du format JSON standardisé ;
  - la consigne de limiter les ingrédients manquants et de répondre en français.

  On l'améliore en modifiant ce fichier, sans toucher au code. Attention : **pas d'accolades** dans ce texte, Spring AI les prend pour des variables de template (un test le vérifie).
- **Modèle et options** dans [application.yml](../../backend/src/main/resources/application.yml) :
  - `LLM_MODEL` (défaut `eu.amazon.nova-lite-v1:0`) ;
  - `temperature: 0.7` et `max-tokens: 1500` ;
  - `antigaspi.recipe.max-ingredients` et `antigaspi.recipe.default-servings`.
- **Identifiants AWS** : `AWS_REGION`, `AWS_ACCESS_KEY_ID` et `AWS_SECRET_ACCESS_KEY` (+ `AWS_SESSION_TOKEN`) dans `.env`. Si on les laisse vides, la configuration `~/.aws` est utilisée (`aws configure` ou `aws sso login`).

## Mise à jour après discussion : ustensiles en base et format de recette

Deux changements ont été décidés en équipe après la première version. Ils sont aussi développés en TDD : les tests ont été modifiés et vus en échec avant le code.

**1. Les ustensiles sont des données, plus une section du profil.**
- Nouveau package `fr.antigaspi.utensil` : `Utensil(id, name)`, `UtensilRepository`, `JsonUtensilRepository` (qui lit [utensils.json](../../backend/src/main/resources/data/utensils.json), 14 ustensiles) et `UtensilService.listAll()` (tri alphabétique).
- Plus tard, une table `utensil (id, name)` en base, via une implémentation JPA de `UtensilRepository`.
- Le profil Markdown ne contient plus que les **préférences** et l'historique des retours. La section `## Équipement` est ignorée.
- Le chargement JSON est maintenant partagé par les deux repositories, dans `fr.antigaspi.common.JsonResources`.

**2. La recette suit un format JSON standardisé**, décrit dans [docs/format-recette.md](../format-recette.md) :

```json
{ "title": "…", "servings": 2, "preparationMinutes": 40,
  "utensils": ["Four"],
  "ingredients": [{ "ingredientId": 3, "name": "Courgettes", "quantity": 0.5, "unit": "kg" }],
  "steps": ["…"] }
```

Les choix faits :
- les clés sont en anglais ;
- la quantité est un nombre plus une unité ;
- il n'y a plus qu'**une seule liste d'ingrédients**. `ingredientId` pointe vers l'aliment du stock, ou vaut `null` pour un produit du placard ou des courses. Il remplace les anciennes listes « utilisés / manquants ».
- `RecipeContext` transmet désormais les ustensiles au LLM ;
- le prompt système décrit le nouveau format.

## Mise à jour : organisation du code par fonctionnalité et par couche

Le découpage par fonctionnalité (`ingredient`, `utensil`, `profile`, `recipe`) est conservé, pour que chacun travaille dans son dossier et limiter les conflits. Chaque fonctionnalité est maintenant découpée en sous-packages par couche :

```
fr.antigaspi
├── ingredient/   model/ (Ingredient, Category, Unit, Urgency, IngredientView) · repository/ · service/
├── utensil/      model/ (Utensil) · repository/ · service/
├── profile/      model/ (UserProfile) · repository/ (Markdown) · service/
├── recipe/       model/ (Recipe, RecipeIngredient, RecipeRequest, RecipeContext) · service/ · llm/ (RecipeGenerator, Spring AI, prompts)
├── common/       utilitaires partagés (JsonResources)
└── config/       configuration Spring (Clock)
```

- Les noms de classes n'ont pas changé, seuls les packages ont bougé. Les tests suivent le même découpage.
- Le profil passe maintenant par un repository, comme l'inventaire et les ustensiles : `ProfileRepository` / `MarkdownProfileRepository`.
- Les règles sont décrites dans [CLAUDE.md](../../CLAUDE.md) (section « Architecture du backend »). Les contrôleurs REST iront dans `<fonctionnalité>/api/`.

## À faire de votre côté

1. **Mettre à jour votre `.env`** à partir du nouveau [.env.example](../../.env.example) :
   - **`LLM_MODEL`** : en région EU, Nova Lite s'appelle via l'inference profile `eu.amazon.nova-lite-v1:0`. L'ancienne valeur `amazon.nova-lite-v1:0` ne fonctionne qu'en `us-east-1`.
   - **`AWS_*`** : ajouter les identifiants AWS.
   - **`PROFILE_PATH=../data/profil.md`** : le backend se lance depuis `backend/`, donc l'ancienne valeur `./data/profil.md` ne trouvera pas le fichier.
2. **Activer l'accès au modèle** Nova Lite dans la console Bedrock, pour la région choisie.
3. Copier le gabarit : `cp data/profil.example.md data/profil.md`, puis adapter les préférences. Une éventuelle section `## Équipement` n'est plus lue : les ustensiles se déclarent dans `utensils.json`.

## Commandes

```bash
mvn verify                                        # build + formatter + 48 tests, sans appel LLM
mvn -q test -pl backend -Dtest=IngredientServiceTest   # une seule classe
mvn formatter:format -pl backend                  # reformater avant commit
mvn test -pl backend -Pllm                        # test live : vrai appel Bedrock (coûte des tokens)
```

Le test live `RecipeGenerationLiveTest` génère une recette avec les aliments les plus urgents et l'affiche dans la console. **Il n'a pas encore été lancé** : il faut des identifiants AWS valides. C'est la première chose à faire pour valider le prompt.

## Tests (48, tous verts)

| Classe | Ce qu'elle vérifie |
|---|---|
| `UrgencyTest` | Seuils 🔴/🟠/🟡/🟢 |
| `IngredientServiceTest` | Tri, jours restants, estimation sans date, départage à date égale, N plus urgents, filtre par ids |
| `JsonIngredientRepositoryTest` | Conversion des jours relatifs, champs absents, fichier invalide, fichier du projet |
| `MarkdownProfileRepositoryTest` / `ProfileServiceTest` | Section Préférences, titres sans accents, puces `*`, repli sur le gabarit, profil vide |
| `JsonUtensilRepositoryTest` / `UtensilServiceTest` | Chargement des ustensiles, fichier invalide, tri alphabétique |
| `RecipePromptBuilderTest` | Contenu et ordre du prompt utilisateur, prompt système sans accolades |
| `SpringAiRecipeGeneratorTest` | Mapping du JSON standardisé → `Recipe`, prompts envoyés, erreurs du LLM (`ChatModel` simulé) |
| `RecipeServiceTest` | Choix des aliments, valeurs par défaut, refus des requêtes invalides sans appel LLM |
| `BackendApplicationTests` | Démarrage du contexte Spring |

## Prochaines étapes proposées

- **Lancer le test live** et ajuster le prompt système selon les recettes obtenues.
- **API REST** : `GET /api/ingredients` et `POST /api/recipes/generate`, de simples façades sur `IngredientService` et `RecipeService`.
- **PostgreSQL** : une implémentation JPA de `IngredientRepository`, avec Flyway et docker-compose, pour remplacer le JSON.
- Trancher les questions ouvertes que ce travail rend concrètes :
  - les durées de conservation par catégorie ;
  - le « placard de base », aujourd'hui géré par le prompt (« ingrédients manquants courants ») ;
  - les conversions d'unités, utiles pour décrémenter le stock après une recette.
- Répartition possible à 3, avec les interfaces comme contrats : (A) API REST + JPA, (B) écriture du profil et boucle de feedback, (C) prompt + consommation.
