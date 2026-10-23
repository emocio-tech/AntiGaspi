package fr.antigaspi.ingredient.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Aliment à ajouter à l'inventaire.
 *
 * @param expiryDate
 *            date de péremption, {@code null} si inconnue (elle sera estimée selon la catégorie)
 */
public record NewIngredient(String name, Category category, BigDecimal quantity, Unit unit, LocalDate expiryDate) {
}
