package fr.antigaspi.recipe.api;

import fr.antigaspi.recipe.model.RecipeIssueType;

/**
 * Anomalie détectée par le garde-fou, telle que le front la reçoit.
 *
 * @param label
 *            libellé français du type d'anomalie
 * @param message
 *            description lisible, avec les noms et identifiants concernés
 */
public record IssueResponse(RecipeIssueType type, String label, String message) {
}
