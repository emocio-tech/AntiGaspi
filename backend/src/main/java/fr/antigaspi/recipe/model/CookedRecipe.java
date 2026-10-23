package fr.antigaspi.recipe.model;

import java.time.Instant;

/**
 * Recette cuisinée par l'utilisateur.
 *
 * @param id
 *            identifiant, nul avant l'enregistrement
 * @param recipe
 *            la recette
 * @param cookedAt
 *            instant où elle a été cuisinée
 * @param rating
 *            avis, facultatif
 * @param comment
 *            commentaire, facultatif
 */
public record CookedRecipe(Long id, Recipe recipe, Instant cookedAt, Rating rating, String comment) {
}
