package fr.antigaspi.recipe.api;

import java.util.List;

import fr.antigaspi.recipe.model.GeneratedRecipe;
import fr.antigaspi.recipe.model.Recipe;

/**
 * Recette générée telle que le front la reçoit.
 *
 * @param recipe
 *            recette au format standardisé (voir {@code docs/format-recette.md}), noms et identifiants corrigés
 * @param issues
 *            anomalies détectées, vide si la recette est conforme
 */
public record RecipeResponse(Recipe recipe, List<IssueResponse> issues) {

    /** Recette générée au format de l'API. */
    public static RecipeResponse from(GeneratedRecipe generated) {
        return new RecipeResponse(generated.recipe(), generated.issues().stream()
                .map(issue -> new IssueResponse(issue.type(), issue.type().label(), issue.message())).toList());
    }
}
