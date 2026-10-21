package fr.antigaspi.ingredient.repository;

import java.util.List;

import fr.antigaspi.ingredient.model.Ingredient;

/**
 * Accès aux aliments de l'inventaire. Implémenté aujourd'hui par un fichier JSON statique, plus tard par PostgreSQL.
 */
public interface IngredientRepository {

    List<Ingredient> findAll();
}
