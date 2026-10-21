# CLAUDE.md — AntiGaspi

Application anti-gaspillage : inventaire d'aliments triés par urgence de péremption et recettes générées par un LLM. Voir le [README](README.md) pour le cadrage complet.

## Stack
- Backend : Java 21, Spring Boot 4, Spring AI 2, Maven multi-module (`pom.xml` agrégateur à la racine, module `backend/`).
- LLM : Spring AI `ChatClient`, isolé derrière l'interface `RecipeGenerator`. Nova Lite via le proxy compatible OpenAI de la formation (starter Spring AI OpenAI ; variables `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL=nova-lite`).
- Données : inventaire et ustensiles en fichiers JSON statiques (`backend/src/main/resources/data/`), derrière des interfaces `*Repository` qui seront implémentées en base (PostgreSQL).
- Profil utilisateur (préférences alimentaires) : fichier Markdown `data/profil.md` (gitignoré), gabarit `data/profil.example.md`.
- Recette : format JSON standardisé (`title`, `servings`, `preparationMinutes`, `utensils`, `ingredients`, `steps`), décrit dans `docs/format-recette.md`.

## Architecture du backend
Découpage **par fonctionnalité**, puis par couche à l'intérieur de chaque fonctionnalité :

```
fr.antigaspi
├── ingredient/   model/ (Ingredient, Category, Unit, Urgency, IngredientView) · repository/ · service/
├── utensil/      model/ (Utensil) · repository/ · service/
├── profile/      model/ (UserProfile) · repository/ (Markdown) · service/
├── recipe/       model/ (Recipe, RecipeIngredient, RecipeRequest, RecipeContext, GeneratedRecipe, RecipeIssue) · service/ (RecipeService, RecipeChecker : garde-fou post-LLM) · llm/ (RecipeGenerator, Spring AI, prompts) · cli/ (démo terminal)
├── common/       utilitaires partagés (JsonResources)
└── config/       configuration Spring (Clock)
```

- `model/` : records et enums métier, sans dépendance à Spring.
- `repository/` : interface `XxxRepository` + implémentation actuelle (`JsonXxxRepository`, `MarkdownProfileRepository`) ; l'implémentation JPA viendra ici.
- `service/` : logique métier, seul point d'entrée pour les futurs `api/` (REST) et MCP.
- `llm/` : tout ce qui dépend du fournisseur de LLM, derrière `RecipeGenerator`.
- Une nouvelle fonctionnalité suit le même schéma ; les contrôleurs REST iront dans `<fonctionnalité>/api/`. Les tests reprennent le package de la classe testée.

## Commandes
- Tous les tests (sans appel LLM) : `mvn -q verify`
- Une classe de test : `mvn -q test -pl backend -Dtest=Classe#methode`
- Formatage : `mvn formatter:format` (le hook pre-commit lance `formatter:validate`)
- Démo en terminal (aliments urgents + 10 recettes JSON + bilan des échecs, appels réels au LLM) : `mvn -pl backend spring-boot:run -Dspring-boot.run.profiles=demo`
- Sans Java, avec Docker : `docker compose build` (compile + tests), puis `docker compose run --rm backend` (démo). Le `.env` n'est jamais copié dans l'image (`.dockerignore`).

## Conventions
- Code, commentaires, doc et messages de commit en français.
- Classe de test `XxxTest` pour la classe `Xxx`, même package, sous `src/test/java` : les hooks retrouvent les tests par ce nom.
- Tests déterministes : pas de réseau, heure figée via un `Clock` injecté, LLM simulé (faux `ChatModel`). Aucun test n'appelle le vrai LLM : la vérification en réel se fait avec la démo.
- Pas de test sans valeur : un test qui ne fait que vérifier une délégation, ou qui n'est jamais exécuté, n'est pas gardé.
- Branches `feat/<nom>` et `fix/<nom>`, PR vers `main`.

## Restrictions
- Ne jamais lire ni modifier `.env` (clés API), par aucun moyen. Seul `.env.example` est lisible.
- Ne jamais committer `.env`, une clé API ou `data/profil.md`.

## Méthode : TDD obligatoire
Toute feature ou correction suit le cycle Red → Green → Refactor :
1. **Red** : écrire d'abord les tests du plan de test validé. Si nécessaire, créer des stubs minimaux (signatures, `throw new UnsupportedOperationException()`) pour que le code compile, sans aucune logique. Lancer les tests et **constater qu'ils échouent** pour la bonne raison.
2. **Green** : écrire le code de production minimal qui fait passer les tests.
3. **Refactor** : nettoyer en gardant les tests verts.

Règles :
- Les tests sont la spécification : interdit de modifier, affaiblir ou désactiver un test pour le faire passer sans accord humain explicite. Un test qui semble faux est remonté à l'utilisateur.
- On ne commite qu'au vert, tests et code ensemble (le hook pre-commit lance les tests ciblés).
- En phase Red, le hook Stop (`stop-tests`) bloque une fois la fin du tour parce que les tests des stubs échouent : c'est attendu, il ne faut pas le contourner.
