package fr.antigaspi.recipe.model;

import java.util.List;

/**
 * Recette générée après contrôle par rapport à l'inventaire.
 *
 * @param recipe
 *            recette, éventuellement corrigée (noms remis, identifiants incohérents retirés)
 * @param issues
 *            anomalies détectées, vide si la recette est conforme
 */
public record GeneratedRecipe(Recipe recipe, List<RecipeIssue> issues) {
}
