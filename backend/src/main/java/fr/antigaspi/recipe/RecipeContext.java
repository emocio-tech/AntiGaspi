package fr.antigaspi.recipe;

import java.util.List;

import fr.antigaspi.ingredient.IngredientView;
import fr.antigaspi.profile.UserProfile;

/**
 * Tout ce que le générateur doit connaître pour proposer une recette.
 *
 * @param ingredients
 *            aliments disponibles, du plus urgent au moins urgent
 * @param maxMinutes
 *            temps maximal, {@code null} si aucune contrainte
 */
public record RecipeContext(List<IngredientView> ingredients, UserProfile profile, int servings, Integer maxMinutes) {
}
