package fr.antigaspi.recipe.api;

import fr.antigaspi.recipe.model.Rating;

/** Corps de {@code PUT /api/cooked-recipes/{id}/feedback} ; un avis ou un commentaire nul est effacé. */
public record FeedbackRequest(Rating rating, String comment) {
}
