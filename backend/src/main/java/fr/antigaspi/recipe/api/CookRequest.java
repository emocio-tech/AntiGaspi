package fr.antigaspi.recipe.api;

import java.util.List;

import fr.antigaspi.recipe.model.Consumption;
import fr.antigaspi.recipe.model.Rating;
import fr.antigaspi.recipe.model.Recipe;

/** Corps de {@code POST /api/cooked-recipes}. */
public record CookRequest(Recipe recipe, Rating rating, String comment, List<Consumption> consumed) {
}
