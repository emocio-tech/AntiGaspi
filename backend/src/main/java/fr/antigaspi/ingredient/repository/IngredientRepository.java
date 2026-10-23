package fr.antigaspi.ingredient.repository;

import java.util.List;

import fr.antigaspi.ingredient.model.Ingredient;

/**
 * Lecture des aliments de l'inventaire. Implémentée par la base MySQL ({@link JpaIngredientRepository}, via
 * {@link MutableIngredientRepository}) ; {@link JsonIngredientRepository} ne fournit plus que les données initiales.
 */
public interface IngredientRepository {

    List<Ingredient> findAll();
}
