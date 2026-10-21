package fr.antigaspi.recipe.model;

import java.util.List;

/**
 * Demande de génération de recette.
 *
 * @param ingredientIds
 *            aliments à utiliser ; vide ou {@code null} pour prendre les plus urgents
 * @param servings
 *            nombre de portions ; {@code null} pour la valeur par défaut
 * @param maxMinutes
 *            temps de préparation maximal ; {@code null} pour aucune contrainte
 */
public record RecipeRequest(List<Long> ingredientIds, Integer servings, Integer maxMinutes) {

    public static RecipeRequest mostUrgent() {
        return new RecipeRequest(List.of(), null, null);
    }
}
