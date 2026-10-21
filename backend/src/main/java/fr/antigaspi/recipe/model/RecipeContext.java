package fr.antigaspi.recipe.model;

import java.util.List;

import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.profile.model.UserProfile;
import fr.antigaspi.utensil.model.Utensil;

/**
 * Tout ce que le générateur doit connaître pour proposer une recette.
 *
 * @param priorityIngredients
 *            aliments à consommer en priorité (les plus urgents, ou ceux choisis), du plus urgent au moins urgent
 * @param otherIngredients
 *            reste de l'inventaire, facultatif pour compléter la recette, du plus urgent au moins urgent
 * @param utensils
 *            ustensiles disponibles
 * @param maxMinutes
 *            temps maximal, {@code null} si aucune contrainte
 */
public record RecipeContext(List<IngredientView> priorityIngredients, List<IngredientView> otherIngredients,
        List<Utensil> utensils, UserProfile profile, int servings, Integer maxMinutes) {
}
