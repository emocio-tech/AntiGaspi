package fr.antigaspi.recipe.model;

import java.util.List;

/**
 * Recette générée par le LLM, au format JSON standardisé (destiné à être stocké en base plus tard).
 *
 * @param title
 *            titre de la recette
 * @param servings
 *            nombre de personnes
 * @param preparationMinutes
 *            temps de préparation estimé, cuisson comprise, en minutes
 * @param utensils
 *            ustensiles utilisés, choisis parmi les ustensiles disponibles
 * @param ingredients
 *            ingrédients avec leur poids ou quantité
 * @param steps
 *            étapes de la recette, dans l'ordre
 */
public record Recipe(String title, int servings, int preparationMinutes, List<String> utensils,
        List<RecipeIngredient> ingredients, List<String> steps) {
}
