package fr.antigaspi.ingredient.api;

import fr.antigaspi.ingredient.model.Category;

/**
 * Catégorie d'aliment proposée comme filtre au front.
 *
 * @param code
 *            code stable, valeur du paramètre {@code category}
 * @param label
 *            libellé français, pour l'affichage
 */
public record CategoryResponse(Category code, String label) {
}
