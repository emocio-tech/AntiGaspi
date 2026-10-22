package fr.antigaspi.ingredient.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Unit;

/**
 * Corps de création ou de modification d'un aliment ; catégorie et unité par code d'énumération.
 *
 * @param expiryDate
 *            date de péremption, facultative
 */
public record IngredientRequest(String name, Category category, BigDecimal quantity, Unit unit, LocalDate expiryDate) {
}
