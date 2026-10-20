package fr.antigaspi.recipe.model;

/**
 * Anomalie détectée par le contrôle d'une recette générée.
 *
 * @param type
 *            nature de l'anomalie
 * @param message
 *            description lisible, avec les noms et identifiants concernés
 */
public record RecipeIssue(RecipeIssueType type, String message) {
}
