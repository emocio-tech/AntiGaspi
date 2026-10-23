package fr.antigaspi.ingredient.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modification d'un aliment de l'inventaire : seuls les champs renseignés ({@code null} = inchangé) sont appliqués.
 *
 * @param id
 *            identifiant de l'aliment à modifier
 * @param expiryDate
 *            nouvelle date de péremption
 * @param clearExpiryDate
 *            {@code true} pour retirer la date de péremption (elle sera de nouveau estimée selon la catégorie) ;
 *            incompatible avec {@code expiryDate}
 */
public record IngredientUpdate(long id, String name, Category category, BigDecimal quantity, Unit unit,
        LocalDate expiryDate, boolean clearExpiryDate) {
}
