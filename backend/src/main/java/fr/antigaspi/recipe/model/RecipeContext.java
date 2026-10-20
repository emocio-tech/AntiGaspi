package fr.antigaspi.recipe.model;

import java.util.List;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.profile.model.UserProfile;
import fr.antigaspi.utensil.model.Utensil;

/**
 * Tout ce que le générateur doit connaître pour proposer une recette.
 *
 * @param ingredients
 *            aliments disponibles, du plus urgent au moins urgent
 * @param utensils
 *            ustensiles disponibles
 * @param maxMinutes
 *            temps maximal, {@code null} si aucune contrainte
 */
public record RecipeContext(List<IngredientView> ingredients, List<Utensil> utensils, UserProfile profile, int servings,
        Integer maxMinutes) {
}
