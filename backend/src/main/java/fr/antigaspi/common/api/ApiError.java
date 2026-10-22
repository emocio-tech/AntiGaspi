package fr.antigaspi.common.api;

/**
 * Corps de réponse d'une requête invalide (400) ou d'une ressource introuvable (404).
 *
 * @param message
 *            explication lisible de l'erreur
 */
public record ApiError(String message) {
}
