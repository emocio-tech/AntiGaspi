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
 * @param instructions
 *            demande de l'utilisateur en texte libre, {@code null} si aucune
 * @param imposedMain
 *            aliment principal imposé à cette variante, {@code null} si aucun
 * @param imposedDishType
 *            type de plat imposé à cette variante, {@code null} sans variante
 * @param recentlyCooked
 *            recettes cuisinées récemment, de la plus récente à la plus ancienne, avec avis et commentaire
 */
public record RecipeContext(List<IngredientView> priorityIngredients, List<IngredientView> otherIngredients,
        List<Utensil> utensils, UserProfile profile, int servings, Integer maxMinutes, String instructions,
        IngredientView imposedMain, DishType imposedDishType, List<CookedRecipe> recentlyCooked) {

    /** Contexte sans variante ni recette cuisinée récemment. */
    public RecipeContext(List<IngredientView> priorityIngredients, List<IngredientView> otherIngredients,
            List<Utensil> utensils, UserProfile profile, int servings, Integer maxMinutes, String instructions) {
        this(priorityIngredients, otherIngredients, utensils, profile, servings, maxMinutes, instructions, null, null,
                List.of());
    }

    /** Contexte avec variante, sans recette cuisinée récemment. */
    public RecipeContext(List<IngredientView> priorityIngredients, List<IngredientView> otherIngredients,
            List<Utensil> utensils, UserProfile profile, int servings, Integer maxMinutes, String instructions,
            IngredientView imposedMain, DishType imposedDishType) {
        this(priorityIngredients, otherIngredients, utensils, profile, servings, maxMinutes, instructions, imposedMain,
                imposedDishType, List.of());
    }
}
