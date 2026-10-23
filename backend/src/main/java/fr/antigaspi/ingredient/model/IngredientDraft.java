package fr.antigaspi.ingredient.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Saisie d'un aliment, avant validation et attribution d'un identifiant.
 *
 * @param expiryDate
 *            date de péremption, {@code null} si inconnue
 */
public record IngredientDraft(String name, Category category, BigDecimal quantity, Unit unit, LocalDate expiryDate) {
}
