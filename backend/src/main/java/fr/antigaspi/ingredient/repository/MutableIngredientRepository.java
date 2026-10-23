package fr.antigaspi.ingredient.repository;

import java.util.Collection;
import java.util.List;

import fr.antigaspi.ingredient.model.Ingredient;

/**
 * Inventaire modifiable : lecture ({@link IngredientRepository}) et écriture. Implémenté par la base de données.
 */
public interface MutableIngredientRepository extends IngredientRepository {

    /**
     * Les aliments portant ces identifiants ; un identifiant inconnu est simplement absent du résultat.
     */
    List<Ingredient> findAllById(Collection<Long> ids);

    /**
     * Enregistre les aliments : un identifiant {@code null} crée l'aliment (l'identifiant est attribué), un identifiant
     * connu le remplace.
     *
     * @return les aliments enregistrés, avec leur identifiant, dans l'ordre reçu
     */
    List<Ingredient> saveAll(List<Ingredient> ingredients);

    void deleteAllById(Collection<Long> ids);
}
