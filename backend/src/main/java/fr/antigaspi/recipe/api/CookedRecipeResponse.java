package fr.antigaspi.recipe.api;

import java.time.Instant;

import fr.antigaspi.recipe.model.Rating;
import fr.antigaspi.recipe.model.Recipe;

/** Recette cuisinée renvoyée au front. */
public record CookedRecipeResponse(long id, String title, Instant cookedAt, Rating rating, String comment,
        Recipe recipe) {
}
