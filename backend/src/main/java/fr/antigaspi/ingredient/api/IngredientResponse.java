package fr.antigaspi.ingredient.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Urgency;

/**
 * Aliment tel que le front le reçoit.
 *
 * @param categoryLabel
 *            libellé français de la catégorie, pour l'affichage
 * @param unit
 *            symbole de l'unité (g, kg, ml, l, pièce)
 * @param expiryDate
 *            date de péremption saisie, ou estimée si absente
 * @param daysLeft
 *            jours restants avant {@code expiryDate} (négatif si périmé)
 * @param estimated
 *            {@code true} si {@code expiryDate} est estimée d'après la catégorie
 * @param urgency
 *            code d'urgence, que le front traduit en couleur
 */
public record IngredientResponse(long id, String name, Category category, String categoryLabel, BigDecimal quantity,
        String unit, LocalDate expiryDate, long daysLeft, boolean estimated, Urgency urgency) {
}
