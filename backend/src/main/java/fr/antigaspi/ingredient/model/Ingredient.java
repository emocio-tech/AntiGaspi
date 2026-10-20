package fr.antigaspi.ingredient.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Aliment de l'inventaire.
 *
 * @param expiryDate
 *            date de péremption saisie, {@code null} si inconnue (elle est alors estimée selon la catégorie)
 * @param addedAt
 *            date d'ajout à l'inventaire
 */
public record Ingredient(Long id, String name, Category category, BigDecimal quantity, Unit unit, LocalDate expiryDate,
        Instant addedAt) {
}
