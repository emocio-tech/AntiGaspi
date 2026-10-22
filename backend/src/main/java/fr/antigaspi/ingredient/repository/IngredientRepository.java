package fr.antigaspi.ingredient.repository;

import java.util.List;

import fr.antigaspi.ingredient.model.Ingredient;

/**
 * Accès aux aliments de l'inventaire, stockés en base (PostgreSQL, H2 en test) et implémenté par
 * {@link JpaIngredientRepository}.
 */
public interface IngredientRepository {

    List<Ingredient> findAll();
}
