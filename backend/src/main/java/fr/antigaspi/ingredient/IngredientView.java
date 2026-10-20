package fr.antigaspi.ingredient;

import java.time.LocalDate;

/**
 * Aliment enrichi des informations de péremption calculées.
 *
 * @param effectiveExpiryDate
 *            date de péremption saisie, ou estimée si absente
 * @param daysLeft
 *            jours restants avant {@code effectiveExpiryDate} (négatif si périmé)
 * @param estimated
 *            {@code true} si la date de péremption est estimée d'après la catégorie
 */
public record IngredientView(Ingredient ingredient, LocalDate effectiveExpiryDate, long daysLeft, Urgency urgency,
        boolean estimated) {
}
