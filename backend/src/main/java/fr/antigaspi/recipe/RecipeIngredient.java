package fr.antigaspi.recipe;

import java.math.BigDecimal;

/**
 * Ingrédient d'une recette.
 *
 * @param ingredientId
 *            identifiant de l'aliment de l'inventaire utilisé, {@code null} si l'ingrédient n'en vient pas (placard,
 *            courses)
 * @param name
 *            nom de l'ingrédient
 * @param quantity
 *            poids ou quantité, exprimé dans {@code unit}
 * @param unit
 *            unité : g, kg, ml, l, pièce…
 */
public record RecipeIngredient(Long ingredientId, String name, BigDecimal quantity, String unit) {
}
