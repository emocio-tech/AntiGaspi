# Format standardisé d'une recette (JSON)

Le LLM renvoie toujours une recette dans ce format. Il correspond au record Java [`Recipe`](../backend/src/main/java/fr/antigaspi/recipe/Recipe.java). Il est pensé pour être **stocké en base** plus tard, sans transformation.

## Champs

| Champ | Type | Obligatoire | Description |
|---|---|---|---|
| `title` | texte | oui | Titre de la recette |
| `servings` | entier > 0 | oui | Nombre de personnes |
| `preparationMinutes` | entier > 0 | oui | Temps de préparation estimé en minutes, cuisson comprise |
| `utensils` | liste de textes | oui | Ustensiles utilisés, avec le nom exact d'un ustensile disponible (table `utensil`) |
| `ingredients` | liste d'objets | oui | Tous les ingrédients de la recette (voir ci-dessous) |
| `steps` | liste de textes | oui | Étapes dans l'ordre, sans numéro dans le texte |

Chaque élément de `ingredients` :

| Champ | Type | Obligatoire | Description |
|---|---|---|---|
| `name` | texte | oui | Nom de l'ingrédient |
| `quantity` | nombre décimal | oui | Poids ou quantité, exprimé dans `unit` |
| `unit` | texte | oui | `g`, `kg`, `ml`, `l`, `pièce`, ou une unité de cuisine (`cuillère à soupe`, `pincée`…) |
| `ingredientId` | entier ou `null` | non | Identifiant de l'aliment de l'inventaire utilisé ; `null` si l'ingrédient vient du placard ou des courses |

`ingredientId` sert à l'écran « J'ai cuisiné cette recette » : grâce à lui, on sait quel stock décrémenter, et de combien. Pour un aliment de l'inventaire, le LLM reprend l'unité de l'inventaire.

## Exemple

```json
{
  "title": "Gratin de courgettes",
  "servings": 2,
  "preparationMinutes": 40,
  "utensils": ["Four", "Plat à gratin", "Couteau de cuisine"],
  "ingredients": [
    { "ingredientId": 3, "name": "Courgettes", "quantity": 0.5, "unit": "kg" },
    { "ingredientId": 7, "name": "Emmental râpé", "quantity": 80, "unit": "g" },
    { "ingredientId": null, "name": "Huile d'olive", "quantity": 1, "unit": "cuillère à soupe" }
  ],
  "steps": [
    "Préchauffer le four à 200 °C.",
    "Couper les courgettes en rondelles.",
    "Les disposer dans le plat, arroser d'huile et couvrir d'emmental.",
    "Enfourner 30 minutes."
  ]
}
```

## Où c'est défini

| Élément | Fichier |
|---|---|
| Structure Java | [Recipe.java](../backend/src/main/java/fr/antigaspi/recipe/Recipe.java), [RecipeIngredient.java](../backend/src/main/java/fr/antigaspi/recipe/RecipeIngredient.java) |
| Consignes au LLM | [recipe-system.md](../backend/src/main/resources/prompts/recipe-system.md) (section « Format de la recette ») |
| Conversion du JSON | Spring AI, sortie structurée : le schéma JSON est déduit du record et envoyé au LLM |
| Test de référence | `SpringAiRecipeGeneratorTest.convertitLaReponseJsonStandardiseeEnRecette` |

Pour modifier le format, il faut changer **ensemble** le record, la section « Format » du prompt système, ce document et le test de référence.

## Stockage en base (proposition, plus tard)

| Table | Colonnes |
|---|---|
| `recipe` | `id`, `title`, `servings`, `preparation_minutes`, `created_at` |
| `recipe_ingredient` | `recipe_id`, `position`, `name`, `quantity`, `unit`, `ingredient_id` (nullable, sans contrainte forte : l'aliment peut avoir été supprimé du stock depuis) |
| `recipe_step` | `recipe_id`, `position`, `text` |
| `recipe_utensil` | `recipe_id`, `utensil_id` |

Variante plus simple : une table `recipe` avec une colonne `content` en `jsonb` qui contient ce JSON tel quel.
