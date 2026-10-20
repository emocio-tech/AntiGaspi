package fr.antigaspi.recipe;

import java.math.BigDecimal;

/**
 * Ingrédient d'une recette.
 *
 * @param ingredientId
 *            identifiant de l'aliment de l'inventaire utilisé, {@code null} pour un ingrédient manquant
 */
public record RecipeIngredient(Long ingredientId, String name, BigDecimal quantity, String unit) {
}
