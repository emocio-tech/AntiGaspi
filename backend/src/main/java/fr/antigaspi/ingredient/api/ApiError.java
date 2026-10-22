package fr.antigaspi.ingredient.api;

/**
 * Corps de réponse d'une requête invalide (400).
 *
 * @param message
 *            explication lisible de l'erreur
 */
public record ApiError(String message) {
}
