package fr.antigaspi.recipe.tool;

import fr.antigaspi.recipe.model.GeneratedRecipe;

/**
 * Événement Spring publié par {@link RecipeTools} chaque fois qu'une recette est générée pour le LLM du chat.
 *
 * @param generated
 *            recette contrôlée et anomalies détectées
 */
public record RecipeGeneratedEvent(GeneratedRecipe generated) {
}
