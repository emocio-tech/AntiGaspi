package fr.antigaspi.recipe.model;

import java.math.BigDecimal;

/**
 * Quantité d'un aliment de l'inventaire utilisée en cuisinant.
 *
 * @param ingredientId
 *            identifiant de l'aliment
 * @param quantity
 *            quantité utilisée, dans l'unité de l'inventaire
 */
public record Consumption(Long ingredientId, BigDecimal quantity) {
}
